package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.dto.SignupAuditDTO;
import com.competition.backend.entity.IndividualSignup;
import com.competition.backend.entity.SignupAudit;
import com.competition.backend.entity.TeamSignup;
import com.competition.backend.repository.IndividualSignupRepository;
import com.competition.backend.repository.SignupAuditRepository;
import com.competition.backend.repository.TeamSignupRepository;
import com.competition.backend.service.AuditService;
import com.competition.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final IndividualSignupRepository individualSignupRepository;
    private final TeamSignupRepository teamSignupRepository;
    private final SignupAuditRepository signupAuditRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSignup(Long adminId, SignupAuditDTO dto) {
        if (!"APPROVED".equals(dto.getResult()) && !"REJECTED".equals(dto.getResult())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核结果只能是 APPROVED 或 REJECTED");
        }

        if ("INDIVIDUAL".equals(dto.getBizType())) {
            processIndividualAudit(dto);
        } else if ("TEAM".equals(dto.getBizType())) {
            processTeamAudit(dto);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的业务类型");
        }

        SignupAudit auditLog = SignupAudit.builder()
                .bizType(dto.getBizType())
                .bizId(dto.getBizId())
                .auditorId(adminId)
                .result(dto.getResult())
                .rejectReason(dto.getRejectReason())
                .build();
        signupAuditRepository.save(auditLog);
    }

    private void processIndividualAudit(SignupAuditDTO dto) {
        IndividualSignup signup = individualSignupRepository.findById(dto.getBizId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SIGNUP_NOT_FOUND, "个人报名记录不存在"));

        if (!"PENDING".equals(signup.getStatus()) && !"RESUBMITTED".equals(signup.getStatus())) {
            throw new BusinessException(ErrorCode.SIGNUP_STATUS_ERROR, "该报名记录当前状态无法审核");
        }

        signup.setStatus(dto.getResult());
        if ("REJECTED".equals(dto.getResult())) {
            signup.setRejectReason(dto.getRejectReason());
        }
        individualSignupRepository.save(signup);

        // 通知学生审核结果
        String notifType = "APPROVED".equals(dto.getResult()) ? "AUDIT_APPROVED" : "AUDIT_REJECTED";
        String notifTitle = "APPROVED".equals(dto.getResult()) ? "报名审核通过" : "报名审核驳回";
        String notifContent = "APPROVED".equals(dto.getResult())
                ? "您的个人赛报名已审核通过，报名正式生效。"
                : "您的个人赛报名被驳回，原因：" + (dto.getRejectReason() != null ? dto.getRejectReason() : "无");
        notificationService.send(signup.getStudentId(), notifType, notifTitle, notifContent, signup.getId());

        // 驳回时通知指导老师
        if ("REJECTED".equals(dto.getResult())) {
            notificationService.send(signup.getTeacherId(), "AUDIT_REJECTED",
                    "学生报名被驳回", "您指导的学生报名被管理员驳回，请查看详情并通知学生修改。", signup.getId());
        }
    }

    private void processTeamAudit(SignupAuditDTO dto) {
        TeamSignup signup = teamSignupRepository.findById(dto.getBizId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SIGNUP_NOT_FOUND, "团队报名记录不存在"));

        if (!"PENDING".equals(signup.getStatus()) && !"RESUBMITTED".equals(signup.getStatus())) {
            throw new BusinessException(ErrorCode.SIGNUP_STATUS_ERROR, "该报名记录当前状态无法审核");
        }

        signup.setStatus(dto.getResult());
        if ("REJECTED".equals(dto.getResult())) {
            signup.setRejectReason(dto.getRejectReason());
        }
        teamSignupRepository.save(signup);

        // 通知指导老师审核结果
        String notifType = "APPROVED".equals(dto.getResult()) ? "AUDIT_APPROVED" : "AUDIT_REJECTED";
        String notifTitle = "APPROVED".equals(dto.getResult()) ? "团队报名审核通过" : "团队报名审核驳回";
        String notifContent = "APPROVED".equals(dto.getResult())
                ? "您指导的队伍报名已审核通过，报名正式生效。"
                : "您指导的队伍报名被驳回，原因：" + (dto.getRejectReason() != null ? dto.getRejectReason() : "无");
        notificationService.send(signup.getTeacherId(), notifType, notifTitle, notifContent, signup.getId());
    }
}