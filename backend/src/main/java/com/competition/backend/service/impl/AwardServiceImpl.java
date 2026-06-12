package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.dto.AwardAuditDTO;
import com.competition.backend.dto.CreateAwardDTO;
import com.competition.backend.entity.AwardAudit;
import com.competition.backend.entity.AwardRecord;
import com.competition.backend.entity.SysUser;
import com.competition.backend.repository.AwardAuditRepository;
import com.competition.backend.repository.AwardRecordRepository;
import com.competition.backend.repository.SysUserRepository;
import com.competition.backend.service.AwardService;
import com.competition.backend.vo.AwardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AwardServiceImpl implements AwardService {

    private final AwardRecordRepository awardRecordRepository;
    private final AwardAuditRepository awardAuditRepository;
    private final SysUserRepository sysUserRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAward(Long userId, CreateAwardDTO dto) {
        // 校验报名类型
        if (!"INDIVIDUAL".equals(dto.getBizType()) && !"TEAM".equals(dto.getBizType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报名类型只能是 INDIVIDUAL 或 TEAM");
        }

        AwardRecord record = AwardRecord.builder()
                .competitionId(dto.getCompetitionId())
                .submitterId(userId)
                .bizType(dto.getBizType())
                .bizId(dto.getBizId())
                .awardLevel(dto.getAwardLevel())
                .awardName(dto.getAwardName())
                .certificateUrl(dto.getCertificateUrl())
                .awardDate(dto.getAwardDate())
                .status("PENDING")
                .build();

        awardRecordRepository.save(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditAward(Long adminId, AwardAuditDTO dto) {
        // 校验审核结果
        if (!"APPROVED".equals(dto.getResult()) && !"REJECTED".equals(dto.getResult())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核结果只能是 APPROVED 或 REJECTED");
        }

        AwardRecord record = awardRecordRepository.findById(dto.getAwardRecordId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AWARD_NOT_FOUND, "获奖记录不存在"));

        // 只有 PENDING 状态才能审核
        if (!"PENDING".equals(record.getStatus())) {
            throw new BusinessException(ErrorCode.AWARD_SIGNUP_NOT_APPROVED, "该获奖记录当前状态无法审核");
        }

        // 更新获奖记录状态
        record.setStatus(dto.getResult());
        awardRecordRepository.save(record);

        // 记录审核流水
        AwardAudit auditLog = AwardAudit.builder()
                .awardRecordId(record.getId())
                .auditorId(adminId)
                .result(dto.getResult())
                .rejectReason(dto.getRejectReason())
                .build();
        awardAuditRepository.save(auditLog);
    }

    @Override
    public Page<AwardVO> getAwardsByCompetition(Long competitionId, Pageable pageable) {
        Page<AwardRecord> records = awardRecordRepository.findByCompetitionId(competitionId, pageable);
        return records.map(this::convertToVO);
    }

    @Override
    public Page<AwardVO> getMyAwards(Long userId, Pageable pageable) {
        Page<AwardRecord> records = awardRecordRepository.findBySubmitterId(userId, pageable);
        return records.map(this::convertToVO);
    }

    private AwardVO convertToVO(AwardRecord record) {
        // 可选：查询提交人姓名
        String submitterName = null;
        try {
            SysUser user = sysUserRepository.findById(record.getSubmitterId()).orElse(null);
            if (user != null) {
                submitterName = user.getRealName();
            }
        } catch (Exception e) {
            // 忽略异常，不影响主流程
        }

        return AwardVO.builder()
                .id(record.getId())
                .competitionId(record.getCompetitionId())
                .submitterId(record.getSubmitterId())
                .submitterName(submitterName)
                .bizType(record.getBizType())
                .bizId(record.getBizId())
                .awardLevel(record.getAwardLevel())
                .awardName(record.getAwardName())
                .certificateUrl(record.getCertificateUrl())
                .awardDate(record.getAwardDate())
                .status(record.getStatus())
                .createdAt(record.getCreatedAt())
                .build();
    }
}