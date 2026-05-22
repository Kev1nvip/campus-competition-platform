package com.competition.backend.service;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.result.PageVO;
import com.competition.backend.entity.Competition;
import com.competition.backend.entity.SysUser;
import com.competition.backend.repository.CompetitionRepository;
import com.competition.backend.repository.SysUserRepository;
import com.competition.backend.service.impl.CompetitionServiceImpl;
import com.competition.backend.util.SecurityUtil;
import com.competition.backend.vo.CompetitionListVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("竞赛模块 - Service 层单元测试")
class CompetitionServiceImplTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private SysUserRepository userRepository;

    @InjectMocks
    private CompetitionServiceImpl competitionService;

    private com.competition.backend.dto.CompetitionSaveDTO buildValidDto() {
        com.competition.backend.dto.CompetitionSaveDTO dto = new com.competition.backend.dto.CompetitionSaveDTO();
        dto.setTitle("蓝桥杯软件大赛");
        dto.setType("INDIVIDUAL");
        dto.setOrganizer("工信部");
        dto.setSignupStart(OffsetDateTime.parse("2026-05-01T00:00:00+08:00"));
        dto.setSignupEnd(OffsetDateTime.parse("2026-06-01T00:00:00+08:00"));
        dto.setCompetitionStart(OffsetDateTime.parse("2026-06-15T00:00:00+08:00"));
        dto.setCompetitionEnd(OffsetDateTime.parse("2026-06-30T00:00:00+08:00"));
        dto.setHasQuota(true);
        dto.setMaxQuota(100);
        dto.setDescription("算法竞赛");
        return dto;
    }

    @Nested
    @DisplayName("2.3 发布竞赛")
    class CreateTests {

        @Test
        @DisplayName("2.3.1 - 正常发布个人赛，返回竞赛ID")
        void create_success_returnsId() {
            com.competition.backend.dto.CompetitionSaveDTO dto = buildValidDto();

            given(competitionRepository.existsByTitleAndTypeAndOrganizerAndCompetitionStartAndStatusNot(
                    dto.getTitle(), dto.getType(), dto.getOrganizer(), dto.getCompetitionStart(), "OFFLINE"))
                    .willReturn(false);
            given(competitionRepository.save(any(Competition.class))).willAnswer(invocation -> {
                Competition c = invocation.getArgument(0);
                c.setId(10L);
                return c;
            });

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCurrentUserId).thenReturn(7L);

                Long id = competitionService.createCompetition(dto);
                assertThat(id).isEqualTo(10L);

                ArgumentCaptor<Competition> captor = ArgumentCaptor.forClass(Competition.class);
                then(competitionRepository).should().save(captor.capture());
                Competition saved = captor.getValue();
                assertThat(saved.getStatus()).isEqualTo("UPCOMING");
                assertThat(saved.getCreatedBy()).isEqualTo(7L);
                assertThat(saved.getEnrolledCount()).isEqualTo(0);
            }
        }

        @Test
        @DisplayName("2.3.2 - 重复发布，抛出 40124")
        void create_fail_duplicateCompetition() {
            com.competition.backend.dto.CompetitionSaveDTO dto = buildValidDto();

            given(competitionRepository.existsByTitleAndTypeAndOrganizerAndCompetitionStartAndStatusNot(
                    dto.getTitle(), dto.getType(), dto.getOrganizer(), dto.getCompetitionStart(), "OFFLINE"))
                    .willReturn(true);

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
                assertThatThrownBy(() -> competitionService.createCompetition(dto))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> {
                            BusinessException be = (BusinessException) ex;
                            assertThat(be.getCode()).isEqualTo(ErrorCode.COMPETITION_EXISTS);
                        });
            }
            then(competitionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("2.3.3 - 报名结束早于开始，抛出 40000")
        void create_fail_signupTimeInvalid() {
            com.competition.backend.dto.CompetitionSaveDTO dto = buildValidDto();
            dto.setSignupEnd(dto.getSignupStart().minusDays(1));

            try (MockedStatic<SecurityUtil> ignored = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                assertThatThrownBy(() -> competitionService.createCompetition(dto))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.PARAM_ERROR));
            }
        }

        @Test
        @DisplayName("2.3.4 - hasQuota=true 且 maxQuota 非法，抛出 40000")
        void create_fail_maxQuotaInvalid() {
            com.competition.backend.dto.CompetitionSaveDTO dto = buildValidDto();
            dto.setMaxQuota(0);

            try (MockedStatic<SecurityUtil> ignored = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                assertThatThrownBy(() -> competitionService.createCompetition(dto))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.PARAM_ERROR));
            }
        }

        @Test
        @DisplayName("2.3.5 - TEAM 且 minTeamSize<2，抛出 40000")
        void create_fail_teamMinSizeInvalid() {
            com.competition.backend.dto.CompetitionSaveDTO dto = buildValidDto();
            dto.setType("TEAM");
            dto.setMinTeamSize(1);

            given(competitionRepository.existsByTitleAndTypeAndOrganizerAndCompetitionStartAndStatusNot(
                    dto.getTitle(), dto.getType(), dto.getOrganizer(), dto.getCompetitionStart(), "OFFLINE"))
                    .willReturn(false);

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
                assertThatThrownBy(() -> competitionService.createCompetition(dto))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.PARAM_ERROR));
            }
        }
    }

    @Nested
    @DisplayName("2.1 竞赛分页列表")
    class ListTests {

        @Test
        @DisplayName("2.1.1 - 正常分页查询返回 PageVO")
        void list_success_returnsPageVO() {
            Competition competition = Competition.builder()
                    .id(1L)
                    .title("竞赛A")
                    .type("INDIVIDUAL")
                    .organizer("主办方A")
                    .status("SIGNING")
                    .signupStart(OffsetDateTime.parse("2026-05-01T00:00:00+08:00"))
                    .signupEnd(OffsetDateTime.parse("2026-05-31T00:00:00+08:00"))
                    .hasQuota(true)
                    .maxQuota(100)
                    .enrolledCount(20)
                    .createdBy(5L)
                    .createdAt(OffsetDateTime.parse("2026-04-01T00:00:00+08:00"))
                    .build();

            Page<Competition> page = new PageImpl<>(List.of(competition), PageRequest.of(0, 10), 1);
            given(competitionRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);
            given(userRepository.findById(5L)).willReturn(Optional.of(SysUser.builder().id(5L).realName("王老师").build()));

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::isAdmin).thenReturn(true);
                PageVO<CompetitionListVO> result = competitionService.getCompetitionList(1, 10, "SIGNING", "INDIVIDUAL", "竞赛");

                assertThat(result.getTotal()).isEqualTo(1);
                assertThat(result.getPage()).isEqualTo(1);
                assertThat(result.getSize()).isEqualTo(10);
                assertThat(result.getList()).hasSize(1);
                assertThat(result.getList().get(0).getRemainingQuota()).isEqualTo(80);
                assertThat(result.getList().get(0).getCreatedByName()).isEqualTo("王老师");
            }
        }
    }

    @Nested
    @DisplayName("2.2 竞赛详情")
    class DetailTests {

        @Test
        @DisplayName("2.2.1 - 存在竞赛时返回详情")
        void detail_success_returnsCompetition() {
            Competition competition = Competition.builder().id(1L).status("SIGNING").createdBy(2L).build();
            given(competitionRepository.findById(1L)).willReturn(Optional.of(competition));

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::isAdmin).thenReturn(false);
                mocked.when(SecurityUtil::getCurrentUserId).thenReturn(2L);
                Competition result = competitionService.getCompetitionDetail(1L);
                assertThat(result.getId()).isEqualTo(1L);
            }
        }

        @Test
        @DisplayName("2.2.2 - 竞赛不存在抛出 40400")
        void detail_fail_notFound() {
            given(competitionRepository.findById(999L)).willReturn(Optional.empty());
            assertThatThrownBy(() -> competitionService.getCompetitionDetail(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("2.2.3 - 已下架且非管理员非创建人不可见")
        void detail_fail_offlineNoPermission() {
            Competition competition = Competition.builder().id(3L).status("OFFLINE").createdBy(99L).build();
            given(competitionRepository.findById(3L)).willReturn(Optional.of(competition));

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::isAdmin).thenReturn(false);
                mocked.when(SecurityUtil::getCurrentUserId).thenReturn(1L);
                assertThatThrownBy(() -> competitionService.getCompetitionDetail(3L))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.NOT_FOUND));
            }
        }
    }

    @Nested
    @DisplayName("2.4 修改竞赛")
    class UpdateTests {

        @Test
        @DisplayName("2.4.1 - 正常更新")
        void update_success() {
            Competition competition = Competition.builder()
                    .id(1L).status("SIGNING").createdBy(11L).title("旧标题").organizer("旧主办方").build();
            given(competitionRepository.findById(1L)).willReturn(Optional.of(competition));
            given(competitionRepository.save(any(Competition.class))).willAnswer(invocation -> invocation.getArgument(0));

            com.competition.backend.dto.CompetitionSaveDTO dto = buildValidDto();
            dto.setTitle("新标题");
            dto.setOrganizer("新主办方");

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(() -> SecurityUtil.checkSelfOrAdmin(11L)).thenAnswer(invocation -> null);
                competitionService.updateCompetition(1L, dto);
            }

            then(competitionRepository).should().save(any(Competition.class));
            assertThat(competition.getTitle()).isEqualTo("新标题");
            assertThat(competition.getOrganizer()).isEqualTo("新主办方");
        }

        @Test
        @DisplayName("2.4.3 - 已结束竞赛不可编辑，抛出 40132")
        void update_fail_finishedCompetition() {
            Competition competition = Competition.builder().id(1L).status("FINISHED").createdBy(11L).build();
            given(competitionRepository.findById(1L)).willReturn(Optional.of(competition));
            com.competition.backend.dto.CompetitionSaveDTO dto = buildValidDto();

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(() -> SecurityUtil.checkSelfOrAdmin(11L)).thenAnswer(invocation -> null);
                assertThatThrownBy(() -> competitionService.updateCompetition(1L, dto))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.SIGNUP_STATUS_ERROR));
            }
            then(competitionRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("2.5 变更竞赛状态")
    class StatusTests {

        @Test
        @DisplayName("2.5.1 - OFFLINE 下架成功")
        void changeStatus_offline_success() {
            Competition competition = Competition.builder().id(1L).status("SIGNING").createdBy(8L).build();
            given(competitionRepository.findById(1L)).willReturn(Optional.of(competition));

            try (MockedStatic<SecurityUtil> mocked = org.mockito.Mockito.mockStatic(SecurityUtil.class)) {
                mocked.when(() -> SecurityUtil.checkSelfOrAdmin(8L)).thenAnswer(invocation -> null);
                competitionService.changeStatus(1L, "OFFLINE");
            }

            assertThat(competition.getStatus()).isEqualTo("OFFLINE");
            then(competitionRepository).should().save(eq(competition));
        }
    }
}
