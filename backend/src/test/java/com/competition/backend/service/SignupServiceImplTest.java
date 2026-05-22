package com.competition.backend.service;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.result.PageVO;
import com.competition.backend.dto.IndividualSignupDTO;
import com.competition.backend.dto.SignupSubmitDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("报名模块 - Service 层单元测试")
class SignupServiceImplTest {

    @Mock
    private IndividualSignupRepository individualSignupRepository;
    @Mock
    private TeamSignupRepository teamSignupRepository;
    @Mock
    private CompetitionRepository competitionRepository;
    @Mock
    private SysUserRepository userRepository;
    @Mock
    private ApplyRecordRepository applyRecordRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private RedisService redisService;

    @InjectMocks
    private SignupServiceImpl signupService;

    private IndividualSignupDTO buildIndividualDto() {
        IndividualSignupDTO dto = new IndividualSignupDTO();
        dto.setCompetitionId(1L);
        dto.setTeacherId(2L);
        dto.setMotivation("希望提升算法能力");
        dto.setIntroduction("掌握Java");
        return dto;
    }

    private Competition buildSigningCompetition() {
        return Competition.builder()
                .id(1L)
                .status("SIGNING")
                .hasQuota(true)
                .maxQuota(100)
                .enrolledCount(10)
                .maxTeachQuota(3)
                .minTeamSize(3)
                .build();
    }

    @Nested
    @DisplayName("3.1 个人赛报名草稿")
    class SignUpIndividualTests {

        @Test
        @DisplayName("3.1.1 - 报名成功返回 signupId 和 DRAFT")
        void signUpIndividual_success() {
            IndividualSignupDTO dto = buildIndividualDto();
            Competition comp = buildSigningCompetition();
            SysUser teacher = SysUser.builder().id(2L).role("TEACHER").build();

            given(competitionRepository.findById(1L)).willReturn(Optional.of(comp));
            given(individualSignupRepository.existsByCompetitionIdAndStudentId(1L, 100L)).willReturn(false);
            given(userRepository.findById(2L)).willReturn(Optional.of(teacher));
            given(redisService.incrTeacherCount(1L, 2L, 3)).willReturn(1L);
            given(redisService.decrCompetitionQuota(1L, 1)).willReturn(89L);
            given(competitionRepository.saveAndFlush(any(Competition.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(individualSignupRepository.save(any(IndividualSignup.class))).willAnswer(invocation -> {
                IndividualSignup s = invocation.getArgument(0);
                s.setId(88L);
                return s;
            });

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCurrentUserId).thenReturn(100L);

                Map<String, Object> result = signupService.signUpIndividual(dto);

                assertThat(result.get("signupId")).isEqualTo(88L);
                assertThat(result.get("status")).isEqualTo("DRAFT");
                then(applyRecordRepository).should().save(any(ApplyRecord.class));
            }
        }

        @Test
        @DisplayName("3.1.3 - 竞赛不在报名期抛 40121")
        void signUpIndividual_fail_notSigning() {
            IndividualSignupDTO dto = buildIndividualDto();
            Competition comp = buildSigningCompetition();
            comp.setStatus("UPCOMING");
            given(competitionRepository.findById(1L)).willReturn(Optional.of(comp));

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
                assertThatThrownBy(() -> signupService.signUpIndividual(dto))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.COMPETITION_NOT_SIGNING));
            }
        }

        @Test
        @DisplayName("3.1.4 - 重复报名抛 40130")
        void signUpIndividual_fail_duplicate() {
            IndividualSignupDTO dto = buildIndividualDto();
            Competition comp = buildSigningCompetition();
            given(competitionRepository.findById(1L)).willReturn(Optional.of(comp));
            given(individualSignupRepository.existsByCompetitionIdAndStudentId(1L, 100L)).willReturn(true);

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
                assertThatThrownBy(() -> signupService.signUpIndividual(dto))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.SIGNUP_DUPLICATE));
            }
        }
    }

    @Nested
    @DisplayName("3.2 提交个人赛审核")
    class SubmitIndividualTests {

        @Test
        @DisplayName("3.2.1 - 提交成功状态置为 PENDING")
        void submitIndividual_success() {
            IndividualSignup signup = IndividualSignup.builder()
                    .id(11L).studentId(100L).status("DRAFT")
                    .motivation("m1").introduction("i1")
                    .build();
            ApplyRecord apply = ApplyRecord.builder().id(9L).status("APPROVED").build();
            SignupSubmitDTO dto = new SignupSubmitDTO();
            dto.setMotivation("m2");
            dto.setIntroduction("i2");

            given(individualSignupRepository.findById(11L)).willReturn(Optional.of(signup));
            given(applyRecordRepository.findByTypeAndBizId("INDIVIDUAL_GUIDE", 11L)).willReturn(Optional.of(apply));

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(() -> SecurityUtil.checkSelf(100L)).thenAnswer(invocation -> null);
                signupService.submitIndividual(11L, dto);
            }

            assertThat(signup.getStatus()).isEqualTo("PENDING");
            assertThat(signup.getMotivation()).isEqualTo("m2");
            then(individualSignupRepository).should().save(eq(signup));
        }

        @Test
        @DisplayName("3.2.3 - 状态非法提交抛 40132")
        void submitIndividual_fail_statusInvalid() {
            IndividualSignup signup = IndividualSignup.builder().id(11L).studentId(100L).status("PENDING").build();
            given(individualSignupRepository.findById(11L)).willReturn(Optional.of(signup));

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(() -> SecurityUtil.checkSelf(100L)).thenAnswer(invocation -> null);
                assertThatThrownBy(() -> signupService.submitIndividual(11L, null))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.SIGNUP_STATUS_ERROR));
            }
            then(individualSignupRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("3.3 我的个人赛记录")
    class MyIndividualTests {

        @Test
        @DisplayName("3.3.1 - 无状态过滤分页查询")
        void myIndividual_withoutStatus() {
            IndividualSignup signup = IndividualSignup.builder().id(1L).studentId(100L).status("DRAFT").build();
            Page<IndividualSignup> page = new PageImpl<>(List.of(signup), PageRequest.of(0, 10), 1);
            given(individualSignupRepository.findByStudentId(100L, PageRequest.of(0, 10))).willReturn(page);

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
                PageVO<IndividualSignup> result = signupService.getMyIndividualSignups(1, 10, null);
                assertThat(result.getTotal()).isEqualTo(1);
                assertThat(result.getList()).hasSize(1);
            }
        }

        @Test
        @DisplayName("3.3.2 - 按状态过滤分页查询")
        void myIndividual_withStatus() {
            IndividualSignup signup = IndividualSignup.builder().id(2L).studentId(100L).status("DRAFT").build();
            Page<IndividualSignup> page = new PageImpl<>(List.of(signup), PageRequest.of(0, 10), 1);
            given(individualSignupRepository.findByStudentIdAndStatus(100L, "DRAFT", PageRequest.of(0, 10))).willReturn(page);

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
                PageVO<IndividualSignup> result = signupService.getMyIndividualSignups(1, 10, "DRAFT");
                assertThat(result.getList()).hasSize(1);
                assertThat(result.getList().get(0).getStatus()).isEqualTo("DRAFT");
            }
        }
    }

    @Nested
    @DisplayName("3.4 创建团队赛报名草稿")
    class SignUpTeamTests {

        @Test
        @DisplayName("3.4.1 - 团队赛草稿创建成功")
        void signUpTeam_success() {
            Team team = Team.builder().id(101L).competitionId(1L).leaderId(100L).teacherId(2L).teacherConfirmed(true).build();
            TeamSignup saved = TeamSignup.builder().id(66L).competitionId(1L).teamId(101L).teacherId(2L).status("DRAFT").build();

            given(teamRepository.findById(101L)).willReturn(Optional.of(team));
            given(teamSignupRepository.existsByCompetitionIdAndTeamId(1L, 101L)).willReturn(false);
            given(teamSignupRepository.save(any(TeamSignup.class))).willReturn(saved);

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(() -> SecurityUtil.checkSelf(100L)).thenAnswer(invocation -> null);
                Map<String, Object> result = signupService.signUpTeam(101L);
                assertThat(result.get("signupId")).isEqualTo(66L);
                assertThat(result.get("status")).isEqualTo("DRAFT");
            }
        }

        @Test
        @DisplayName("3.4.3 - 指导老师未确认抛 40145")
        void signUpTeam_fail_teacherNotConfirmed() {
            Team team = Team.builder().id(101L).competitionId(1L).leaderId(100L).teacherConfirmed(false).build();
            given(teamRepository.findById(101L)).willReturn(Optional.of(team));

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(() -> SecurityUtil.checkSelf(100L)).thenAnswer(invocation -> null);
                assertThatThrownBy(() -> signupService.signUpTeam(101L))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.TEAM_TEACHER_NOT_CONFIRMED));
            }
        }
    }

    @Nested
    @DisplayName("3.5 提交团队赛审核")
    class SubmitTeamTests {

        @Test
        @DisplayName("3.5.1 - 提交成功状态更新")
        void submitTeam_success() {
            TeamSignup signup = TeamSignup.builder().id(8L).competitionId(1L).teamId(101L).status("DRAFT").build();
            Team team = Team.builder().id(101L).leaderId(100L).memberCount(3).status("FORMING").build();
            Competition competition = Competition.builder().id(1L).minTeamSize(3).build();

            given(teamSignupRepository.findById(8L)).willReturn(Optional.of(signup));
            given(teamRepository.findById(101L)).willReturn(Optional.of(team));
            given(competitionRepository.findById(1L)).willReturn(Optional.of(competition));

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(() -> SecurityUtil.checkSelf(100L)).thenAnswer(invocation -> null);
                signupService.submitTeam(8L);
            }

            assertThat(signup.getStatus()).isEqualTo("PENDING");
            assertThat(team.getStatus()).isEqualTo("SUBMITTED");
            then(teamSignupRepository).should().save(eq(signup));
            then(teamRepository).should().save(eq(team));
        }

        @Test
        @DisplayName("3.5.3 - 队伍人数不足抛 40000")
        void submitTeam_fail_memberCountInsufficient() {
            TeamSignup signup = TeamSignup.builder().id(8L).competitionId(1L).teamId(101L).status("DRAFT").build();
            Team team = Team.builder().id(101L).leaderId(100L).memberCount(2).build();
            Competition competition = Competition.builder().id(1L).minTeamSize(3).build();

            given(teamSignupRepository.findById(8L)).willReturn(Optional.of(signup));
            given(teamRepository.findById(101L)).willReturn(Optional.of(team));
            given(competitionRepository.findById(1L)).willReturn(Optional.of(competition));

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(() -> SecurityUtil.checkSelf(100L)).thenAnswer(invocation -> null);
                assertThatThrownBy(() -> signupService.submitTeam(8L))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.PARAM_ERROR));
            }
            then(teamSignupRepository).should(never()).save(any());
        }
    }
}
