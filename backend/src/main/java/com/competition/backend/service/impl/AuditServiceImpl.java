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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final IndividualSignupRepository individualSignupRepository;
    private final TeamSignupRepository teamSignupRepository;
    private final SignupAuditRepository signupAuditRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSignup(Long adminId, SignupAuditDTO dto) {
        // 1. 校验状态参数
        if (!"APPROVED".equals(dto.getResult()) && !"REJECTED".equals(dto.getResult())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核结果只能是 APPROVED 或 REJECTED");
        }

        // 2. 根据 bizType 处理个人或团队
        if ("INDIVIDUAL".equals(dto.getBizType())) {
            processIndividualAudit(dto);
        } else if ("TEAM".equals(dto.getBizType())) {
            processTeamAudit(dto);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的业务类型");
        }

        // 3. 记录审核流水表
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

        // 只有 PENDING 或 RESUBMITTED 状态才能审核
        if (!"PENDING".equals(signup.getStatus()) && !"RESUBMITTED".equals(signup.getStatus())) {
            throw new BusinessException(ErrorCode.SIGNUP_STATUS_ERROR, "该报名记录当前状态无法审核");
        }

        signup.setStatus(dto.getResult());
        if ("REJECTED".equals(dto.getResult())) {
            signup.setRejectReason(dto.getRejectReason());
        }
        individualSignupRepository.save(signup);
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
    }
}