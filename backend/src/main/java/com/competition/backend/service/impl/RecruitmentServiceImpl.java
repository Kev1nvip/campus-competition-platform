package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.dto.ApplyRecruitmentDTO;
import com.competition.backend.dto.AuditApplyDTO;
import com.competition.backend.dto.CreateTeacherRecruitmentDTO;
import com.competition.backend.entity.ApplyRecord;
import com.competition.backend.entity.Competition;
import com.competition.backend.entity.TeacherRecruitment;
import com.competition.backend.repository.ApplyRecordRepository;
import com.competition.backend.repository.CompetitionRepository;
import com.competition.backend.repository.TeacherRecruitmentRepository;
import com.competition.backend.service.RecruitmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecruitmentServiceImpl implements RecruitmentService {

    private final TeacherRecruitmentRepository teacherRecruitmentRepository;
    private final ApplyRecordRepository applyRecordRepository;
    private final CompetitionRepository competitionRepository;

    // =============================================
    // 接口1：发布招募帖（教师）
    // =============================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTeacherRecruitment(Long teacherId,
                                         CreateTeacherRecruitmentDTO dto) {

        // 1. 校验竞赛存在
        Competition competition = competitionRepository
                .findById(dto.getCompetitionId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.COMPETITION_NOT_FOUND, "竞赛不存在"));

// 2. 校验竞赛状态
        if (!"UPCOMING".equals(competition.getStatus()) && !"SIGNING".equals(competition.getStatus())) {
            throw new BusinessException(ErrorCode.COMPETITION_NOT_SIGNING, "竞赛不在报名时间内，不可发布招募帖");
        }

        // 3. 校验招募人数
        if (dto.getRecruitCount() == null || dto.getRecruitCount() <= 0) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR, "招募人数必须大于0");
        }

        // 4. 创建招募帖
        TeacherRecruitment recruitment = TeacherRecruitment.builder()
                .competitionId(competition.getId())
                .teacherId(teacherId)
                .recruitCount(dto.getRecruitCount())
                .currentCount(0)
                .requirement(dto.getRequirement())
                .deadline(dto.getDeadline())
                .status("OPEN")
                .build();

        teacherRecruitmentRepository.save(recruitment);
    }

    // =============================================
    // 接口2：关闭招募帖（仅发布老师可关闭）
    // =============================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeTeacherRecruitment(Long teacherId, Long recruitmentId) {

        TeacherRecruitment recruitment = teacherRecruitmentRepository
                .findById(recruitmentId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "招募帖不存在"));

        if (!recruitment.getTeacherId().equals(teacherId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN, "只有发布者可关闭");
        }

        if ("CLOSED".equals(recruitment.getStatus())) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR, "招募帖已关闭");
        }

        recruitment.setStatus("CLOSED");
        teacherRecruitmentRepository.save(recruitment);
    }

    // =============================================
    // 接口3：学生申请加入老师招募
    // =============================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyTeacherRecruitment(Long studentId,
                                        Long recruitmentId,
                                        ApplyRecruitmentDTO dto) {

        TeacherRecruitment recruitment = teacherRecruitmentRepository
                .findById(recruitmentId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "招募帖不存在"));

        if (!"OPEN".equals(recruitment.getStatus())) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR, "招募帖不可申请");
        }

        if (recruitment.getCurrentCount() >= recruitment.getRecruitCount()) {
            throw new BusinessException(
                    ErrorCode.TEACHER_QUOTA_FULL, "名额已满");
        }

        // 创建申请记录
        ApplyRecord apply = ApplyRecord.builder()
                .type("TEACHER_RECRUIT_APPLY")
                .applicantId(studentId)
                .receiverId(recruitment.getTeacherId())
                .bizId(recruitment.getId())
                .introduction(dto.getIntroduction())
                .motivation(dto.getMotivation())
                .status("PENDING")
                .build();

        applyRecordRepository.save(apply);
    }

    // =============================================
    // 接口4：审核申请（同意/驳回）
    // =============================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditApply(Long teacherId, Long applyId, AuditApplyDTO dto) {

        ApplyRecord apply = applyRecordRepository
                .findById(applyId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.APPLY_NOT_FOUND, "申请记录不存在"));

        if (!apply.getReceiverId().equals(teacherId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN, "只有接收人可审核");
        }

        if (!"PENDING".equals(apply.getStatus())) {
            throw new BusinessException(
                    ErrorCode.APPLY_ALREADY_HANDLED, "申请已处理");
        }

        if ("APPROVED".equals(dto.getStatus())) {
            apply.setStatus("APPROVED");

            // 更新招募帖人数
            TeacherRecruitment recruitment = teacherRecruitmentRepository
                    .findById(apply.getBizId())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.NOT_FOUND, "招募帖不存在"));

            recruitment.setCurrentCount(recruitment.getCurrentCount() + 1);

            if (recruitment.getCurrentCount() >= recruitment.getRecruitCount()) {
                recruitment.setStatus("FULL");
            }

            teacherRecruitmentRepository.save(recruitment);

        } else if ("REJECTED".equals(dto.getStatus())) {
            apply.setStatus("REJECTED");
            apply.setRejectReason(dto.getReason());
        } else {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR, "状态非法");
        }

        applyRecordRepository.save(apply);
    }
}