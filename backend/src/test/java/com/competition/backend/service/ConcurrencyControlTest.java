package com.competition.backend.service;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.dto.IndividualSignupDTO;
import com.competition.backend.entity.*;
import com.competition.backend.repository.*;
import com.competition.backend.service.impl.SignupServiceImpl;
import com.competition.backend.util.SecurityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("第六部分 6.2/6.3 - 并发名额扣减与乐观锁冲突测试")
class ConcurrencyControlTest {

    @Mock private IndividualSignupRepository individualSignupRepository;
    @Mock private TeamSignupRepository teamSignupRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private SysUserRepository userRepository;
    @Mock private ApplyRecordRepository applyRecordRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private RedisService redisService;

    @InjectMocks
    private SignupServiceImpl signupService;

    private IndividualSignupDTO buildDto() {
        IndividualSignupDTO dto = new IndividualSignupDTO();
        dto.setCompetitionId(1L);
        dto.setTeacherId(2L);
        dto.setMotivation("参赛动机");
        dto.setIntroduction("个人介绍");
        return dto;
    }

    private Competition buildComp() {
        return Competition.builder()
                .id(1L).status("SIGNING")
                .hasQuota(true).maxQuota(10).enrolledCount(9)
                .maxTeachQuota(null) // 不限老师带队数，简化测试
                .build();
    }

    private SysUser buildTeacher() {
        return SysUser.builder().id(2L).role("TEACHER").build();
    }

    // ------------------------------------------------------------------ 6.2
    @Nested
    @DisplayName("6.2 并发名额扣减")
    class ConcurrentQuotaDecr {

        @Test
        @DisplayName("6.2.1 名额不足时所有线程均抛 COMPETITION_QUOTA_FULL，Redis 无需回滚（扣减未发生）")
        void allThreadsRejected_whenQuotaExhausted() {
            // decrCompetitionQuota 返回 -2 表示名额不足，此时 compQuotaDeced=false，不触发回滚
            Competition comp = buildComp();
            SysUser teacher = buildTeacher();

            given(competitionRepository.findById(1L)).willReturn(Optional.of(comp));
            given(individualSignupRepository.existsByCompetitionIdAndStudentId(anyLong(), anyLong()))
                    .willReturn(false);
            given(userRepository.findById(2L)).willReturn(Optional.of(teacher));
            given(redisService.decrCompetitionQuota(1L, 1)).willReturn(-2L);

            int threads = 3;
            AtomicInteger quotaFullCount = new AtomicInteger();

            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCurrentUserId).thenReturn(100L);

                for (int i = 0; i < threads; i++) {
                    try {
                        signupService.signUpIndividual(buildDto());
                    } catch (BusinessException ex) {
                        if (ex.getCode() == ErrorCode.COMPETITION_QUOTA_FULL) {
                            quotaFullCount.incrementAndGet();
                        }
                    }
                }
            }

            assertThat(quotaFullCount.get()).isEqualTo(threads);
            // 名额未实际扣减（-2 表示不足，Lua 脚本未执行 decrby），无需回滚
            then(redisService).should(times(0)).incrCompetitionQuota(anyLong(), anyInt());
        }

        @Test
        @DisplayName("6.2.2 首次成功扣减，后续名额不足被拒绝；成功路径无回滚，失败路径无回滚")
        void firstSucceeds_restRejected() {
            Competition comp = buildComp();
            SysUser teacher = buildTeacher();

            given(competitionRepository.findById(1L)).willReturn(Optional.of(comp));
            given(individualSignupRepository.existsByCompetitionIdAndStudentId(anyLong(), anyLong()))
                    .willReturn(false);
            given(userRepository.findById(2L)).willReturn(Optional.of(teacher));
            // 第 1 次成功（返回剩余 8），第 2、3 次名额不足（返回 -2）
            given(redisService.decrCompetitionQuota(1L, 1))
                    .willReturn(8L)
                    .willReturn(-2L)
                    .willReturn(-2L);
            given(competitionRepository.saveAndFlush(any(Competition.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(individualSignupRepository.save(any(IndividualSignup.class)))
                    .willAnswer(inv -> {
                        IndividualSignup s = inv.getArgument(0);
                        s.setId(1L);
                        return s;
                    });

            AtomicInteger successCount = new AtomicInteger();
            AtomicInteger quotaFullCount = new AtomicInteger();

            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCurrentUserId).thenReturn(100L);

                for (int i = 0; i < 3; i++) {
                    try {
                        signupService.signUpIndividual(buildDto());
                        successCount.incrementAndGet();
                    } catch (BusinessException ex) {
                        if (ex.getCode() == ErrorCode.COMPETITION_QUOTA_FULL) {
                            quotaFullCount.incrementAndGet();
                        }
                    }
                }
            }

            assertThat(successCount.get()).isEqualTo(1);
            assertThat(quotaFullCount.get()).isEqualTo(2);
            // 失败路径 compQuotaDeced=false，无回滚
            then(redisService).should(times(0)).incrCompetitionQuota(anyLong(), anyInt());
        }
    }

    // ------------------------------------------------------------------ 6.3
    @Nested
    @DisplayName("6.3 乐观锁冲突测试")
    class OptimisticLockConflict {

        @Test
        @DisplayName("6.3.1 saveAndFlush 抛乐观锁异常 → 业务异常 CONFLICT，Redis 名额与老师计数均回滚")
        void optimisticLock_conflict_rollsBackRedisAndThrowsConflict() {
            Competition comp = buildComp();
            comp.setMaxTeachQuota(3); // 开启老师带队限制，使 teacherCountInced=true
            SysUser teacher = buildTeacher();

            given(competitionRepository.findById(1L)).willReturn(Optional.of(comp));
            given(individualSignupRepository.existsByCompetitionIdAndStudentId(anyLong(), anyLong()))
                    .willReturn(false);
            given(userRepository.findById(2L)).willReturn(Optional.of(teacher));
            given(redisService.incrTeacherCount(1L, 2L, 3)).willReturn(1L);
            given(redisService.decrCompetitionQuota(1L, 1)).willReturn(8L);
            given(competitionRepository.saveAndFlush(any(Competition.class)))
                    .willThrow(new ObjectOptimisticLockingFailureException(Competition.class, 1L));

            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCurrentUserId).thenReturn(100L);

                assertThatThrownBy(() -> signupService.signUpIndividual(buildDto()))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> {
                            BusinessException be = (BusinessException) ex;
                            assertThat(be.getCode()).isEqualTo(ErrorCode.CONFLICT);
                            assertThat(be.getMessage()).contains("请稍后重试");
                        });
            }

            // 名额回滚
            then(redisService).should().incrCompetitionQuota(1L, 1);
            // 老师带队计数回滚
            then(redisService).should().decrTeacherCount(1L, 2L);
        }
    }
}
