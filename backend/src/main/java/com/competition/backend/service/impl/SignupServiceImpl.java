package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.result.PageVO;
import com.competition.backend.dto.IndividualSignupDTO;
import com.competition.backend.dto.SignupSubmitDTO;
import com.competition.backend.entity.*;
import com.competition.backend.repository.*;
import com.competition.backend.service.RedisService;
import com.competition.backend.service.SignupService;
import com.competition.backend.service.NotificationService;
import com.competition.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

    private final IndividualSignupRepository individualSignupRepository;
    private final TeamSignupRepository teamSignupRepository;
    private final CompetitionRepository competitionRepository;
    private final SysUserRepository userRepository;
    private final ApplyRecordRepository applyRecordRepository;
    private final TeamRepository teamRepository;
    private final RedisService redisService;
    private final NotificationService notificationService;

        @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> signUpIndividual(IndividualSignupDTO dto) {
        Long studentId = SecurityUtil.getCurrentUserId();

        // 1. 校验竞赛状态
        Competition comp = competitionRepository.findById(dto.getCompetitionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPETITION_NOT_FOUND, "竞赛不存在"));
        
        if (!"SIGNING".equals(comp.getStatus())) {
            throw new BusinessException(ErrorCode.COMPETITION_NOT_SIGNING, "竞赛不在报名时间内");
        }

        // 2. 校验是否已报名
        if (individualSignupRepository.existsByCompetitionIdAndStudentId(dto.getCompetitionId(), studentId)) {
            throw new BusinessException(ErrorCode.SIGNUP_DUPLICATE, "您已报名该竞赛");
        }

        // 3. 校验老师角色
        SysUser teacher = userRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "老师不存在"));
        if (!"TEACHER".equals(teacher.getRole())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "选择的用户不是老师");
        }

        // =============================================
        // 4. 并发名额校验 (Redis Lua 脚本)
        // =============================================
        boolean teacherCountInced = false;
        boolean compQuotaDeced = false;

        try {
            // 4.1 校验并增加老师带队数
            if (comp.getMaxTeachQuota() != null) {
                Long count = redisService.incrTeacherCount(comp.getId(), dto.getTeacherId(), comp.getMaxTeachQuota());
                if (count == -1) {
                    throw new BusinessException(ErrorCode.TEACHER_QUOTA_FULL, "该老师带队名额已满");
                }
                teacherCountInced = true;
            }

            // 4.2 校验并扣减竞赛名额
            if (Boolean.TRUE.equals(comp.getHasQuota())) {
                Long remaining = redisService.decrCompetitionQuota(comp.getId(), 1);
                if (remaining == -1) { // 缓存失效，尝试从数据库恢复并重新执行
                    redisService.initCompetitionQuota(comp.getId(), comp.getMaxQuota() - comp.getEnrolledCount());
                    remaining = redisService.decrCompetitionQuota(comp.getId(), 1);
                }
                if (remaining == -2) {
                    throw new BusinessException(ErrorCode.COMPETITION_QUOTA_FULL, "竞赛名额已满");
                }
                compQuotaDeced = true;
            }

            // 4.3 数据库乐观锁校验与名额同步
            // 这里修改 comp 对象的 enrolledCount 会触发 JPA 的 version 检查
            comp.setEnrolledCount(comp.getEnrolledCount() + 1);
            competitionRepository.saveAndFlush(comp);

        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            // 乐观锁异常：多个线程同时更新同一行，回滚 Redis
            if (teacherCountInced) redisService.decrTeacherCount(comp.getId(), dto.getTeacherId());
            if (compQuotaDeced) redisService.incrCompetitionQuota(comp.getId(), 1);
            throw new BusinessException(ErrorCode.CONFLICT, "当前报名人数较多，请稍后重试");
        } catch (Exception e) {
            // 业务异常（名额满等）：回滚 Redis
            if (teacherCountInced) redisService.decrTeacherCount(comp.getId(), dto.getTeacherId());
            if (compQuotaDeced) redisService.incrCompetitionQuota(comp.getId(), 1);
            throw e;
        }

        // =============================================
        // 5. 创建报名草稿与申请记录
        // =============================================
        IndividualSignup signup = IndividualSignup.builder()
                .competitionId(dto.getCompetitionId())
                .studentId(studentId)
                .teacherId(dto.getTeacherId())
                .motivation(dto.getMotivation())
                .introduction(dto.getIntroduction())
                .status("DRAFT")
                .build();
        IndividualSignup savedSignup = individualSignupRepository.save(signup);

        // 6. 发送指导申请，并通知老师
        ApplyRecord apply = ApplyRecord.builder()
                .type("INDIVIDUAL_GUIDE")
                .applicantId(studentId)
                .receiverId(dto.getTeacherId())
                .bizId(savedSignup.getId())
                .motivation(dto.getMotivation())
                .status("PENDING")
                .build();
        ApplyRecord savedApply = applyRecordRepository.save(apply);

        String studentName = userRepository.findById(studentId).map(u -> u.getRealName()).orElse("同学");
        String compTitle = comp.getTitle();
        notificationService.send(dto.getTeacherId(), "APPLY_RECEIVED", "收到指导申请",
                "「" + studentName + "」申请你指导参赛竞赛「" + compTitle + "」",
                savedApply.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("signupId", savedSignup.getId());
        result.put("status", "DRAFT");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitIndividual(Long id, SignupSubmitDTO dto) {
        IndividualSignup signup = individualSignupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SIGNUP_NOT_FOUND, "报名记录不存在"));
        
        SecurityUtil.checkSelf(signup.getStudentId());

        // 校验状态
        if (!"DRAFT".equals(signup.getStatus()) && !"REJECTED".equals(signup.getStatus())) {
            throw new BusinessException(ErrorCode.SIGNUP_STATUS_ERROR, "当前状态不允许提交");
        }

        // 校验老师是否同意
        ApplyRecord apply = applyRecordRepository.findByTypeAndBizId("INDIVIDUAL_GUIDE", id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "未找到关联的指导申请"));
        if (!"APPROVED".equals(apply.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "指导老师尚未同意您的申请");
        }

        // 更新信息
        if (dto != null) {
            if (dto.getMotivation() != null) signup.setMotivation(dto.getMotivation());
            if (dto.getIntroduction() != null) signup.setIntroduction(dto.getIntroduction());
        }

        signup.setStatus("PENDING");
        signup.setSubmittedAt(OffsetDateTime.now());
        individualSignupRepository.save(signup);

        // 通知管理员：个人赛报名已提交审核
        String studentName = userRepository.findById(signup.getStudentId()).map(u -> u.getRealName()).orElse("学生");
        String compTitle = competitionRepository.findById(signup.getCompetitionId()).map(c -> c.getTitle()).orElse("竞赛");
        // 通知所有管理员（通过 role=ADMIN 批量查询）
        userRepository.findAll().stream()
                .filter(u -> "ADMIN".equals(u.getRole()) && "ACTIVE".equals(u.getStatus()))
                .forEach(admin -> notificationService.send(admin.getId(), "AUDIT_SUBMITTED", "个人赛报名待审核",
                        "「" + studentName + "」提交了「" + compTitle + "」个人赛报名，请前往审核",
                        signup.getId()));
    }

    @Override
    public PageVO<IndividualSignup> getMyIndividualSignups(int page, int size, String status) {
        Long userId = SecurityUtil.getCurrentUserId();
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<IndividualSignup> signupPage;
        if (status != null) {
            signupPage = individualSignupRepository.findByStudentIdAndStatus(userId, status, pageRequest);
        } else {
            signupPage = individualSignupRepository.findByStudentId(userId, pageRequest);
        }
        return PageVO.of(signupPage);
    }

    @Override
    public IndividualSignup getIndividualDetail(Long id) {
        return individualSignupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SIGNUP_NOT_FOUND, "报名不存在"));
    }

@Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> signUpTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND, "队伍不存在"));
        
        SecurityUtil.checkSelf(team.getLeaderId());

        Competition comp = competitionRepository.findById(team.getCompetitionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPETITION_NOT_FOUND, "竞赛不存在"));
        if (!"UPCOMING".equals(comp.getStatus()) && !"SIGNING".equals(comp.getStatus())) {
            throw new BusinessException(ErrorCode.COMPETITION_NOT_SIGNING, "竞赛不在报名时间内，不可创建报名");
        }

        if (!Boolean.TRUE.equals(team.getTeacherConfirmed())) {
            throw new BusinessException(ErrorCode.TEAM_TEACHER_NOT_CONFIRMED, "指导老师尚未确认，不可报名");
        }

        if (teamSignupRepository.existsByCompetitionIdAndTeamId(team.getCompetitionId(), teamId)) {
            throw new BusinessException(ErrorCode.SIGNUP_DUPLICATE, "该队伍已创建过报名记录");
        }

        TeamSignup signup = TeamSignup.builder()
                .competitionId(team.getCompetitionId())
                .teamId(teamId)
                .teacherId(team.getTeacherId())
                .status("DRAFT")
                .build();
        TeamSignup saved = teamSignupRepository.save(signup);

        Map<String, Object> result = new HashMap<>();
        result.put("signupId", saved.getId());
        result.put("status", "DRAFT");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTeam(Long id) {
        TeamSignup signup = teamSignupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SIGNUP_NOT_FOUND, "报名记录不存在"));
        
        Team team = teamRepository.findById(signup.getTeamId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND, "队伍不存在"));
        SecurityUtil.checkSelf(team.getLeaderId());

Competition comp = competitionRepository.findById(signup.getCompetitionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPETITION_NOT_FOUND, "竞赛不存在"));

        if (!"SIGNING".equals(comp.getStatus())) {
            throw new BusinessException(ErrorCode.COMPETITION_NOT_SIGNING, "竞赛报名已截止，不可提交报名");
        }

        // 校验人数
        if (team.getMemberCount() < comp.getMinTeamSize()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍人数不足，要求至少" + comp.getMinTeamSize() + "人");
        }

        signup.setStatus("PENDING");
        signup.setSubmittedAt(OffsetDateTime.now());
        teamSignupRepository.save(signup);

        team.setStatus("SUBMITTED");
        teamRepository.save(team);

        // 通知管理员：团队赛报名已提交审核
        String leaderName = userRepository.findById(team.getLeaderId()).map(u -> u.getRealName()).orElse("队长");
        String compTitle2 = comp.getTitle();
        userRepository.findAll().stream()
                .filter(u -> "ADMIN".equals(u.getRole()) && "ACTIVE".equals(u.getStatus()))
                .forEach(admin -> notificationService.send(admin.getId(), "AUDIT_SUBMITTED", "团队赛报名待审核",
                        "「" + leaderName + "」队伍提交了「" + compTitle2 + "」团队赛报名，请前往审核",
                        signup.getId()));
    }
}