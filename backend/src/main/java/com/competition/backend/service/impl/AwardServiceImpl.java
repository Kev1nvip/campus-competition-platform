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
import com.competition.backend.repository.IndividualSignupRepository;
import com.competition.backend.repository.SysUserRepository;
import com.competition.backend.repository.TeamSignupRepository;
import com.competition.backend.service.AwardService;
import com.competition.backend.service.NotificationService;
import com.competition.backend.repository.CompetitionRepository;
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
    private final IndividualSignupRepository individualSignupRepository;
    private final TeamSignupRepository teamSignupRepository;
    private final NotificationService notificationService;
    private final CompetitionRepository competitionRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAward(Long userId, CreateAwardDTO dto) {
        if (!"INDIVIDUAL".equals(dto.getBizType()) && !"TEAM".equals(dto.getBizType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报名类型只能是 INDIVIDUAL 或 TEAM");
        }

        // P2-6: 校验关联报名是否已 APPROVED
        if ("INDIVIDUAL".equals(dto.getBizType())) {
            individualSignupRepository.findById(dto.getBizId()).ifPresent(signup -> {
                if (!"APPROVED".equals(signup.getStatus())) {
                    throw new BusinessException(ErrorCode.AWARD_SIGNUP_NOT_APPROVED, "报名未审核通过，不可提交获奖记录");
                }
            });
        } else {
            teamSignupRepository.findById(dto.getBizId()).ifPresent(signup -> {
                if (!"APPROVED".equals(signup.getStatus())) {
                    throw new BusinessException(ErrorCode.AWARD_SIGNUP_NOT_APPROVED, "报名未审核通过，不可提交获奖记录");
                }
            });
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
        AwardRecord saved = awardRecordRepository.save(record);

        // 通知管理员：有获奖记录待审核
        String submitterName = sysUserRepository.findById(userId).map(u -> u.getRealName()).orElse("用户");
        String compTitle = competitionRepository.findById(dto.getCompetitionId()).map(c -> c.getTitle()).orElse("竞赛");
        sysUserRepository.findAll().stream()
                .filter(u -> "ADMIN".equals(u.getRole()) && "ACTIVE".equals(u.getStatus()))
                .forEach(admin -> notificationService.send(admin.getId(), "AWARD_SUBMITTED", "获奖记录待审核",
                        "「" + submitterName + "」提交了「" + compTitle + "」获奖记录，请前往审核",
                        saved.getId()));
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

        // 通知提交人审核结果
        String notifTitle = "APPROVED".equals(dto.getResult()) ? "获奖记录审核通过" : "获奖记录审核驳回";
        String notifContent = "APPROVED".equals(dto.getResult())
                ? "你提交的获奖记录「" + record.getAwardName() + "」已审核通过"
                : "你提交的获奖记录「" + record.getAwardName() + "」被驳回" +
                  (dto.getRejectReason() != null ? "，原因：" + dto.getRejectReason() : "");
        notificationService.send(record.getSubmitterId(),
                "APPROVED".equals(dto.getResult()) ? "AWARD_APPROVED" : "AWARD_REJECTED",
                notifTitle, notifContent, record.getId());
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