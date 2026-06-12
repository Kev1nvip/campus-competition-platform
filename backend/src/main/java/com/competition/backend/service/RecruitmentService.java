package com.competition.backend.service;

import com.competition.backend.dto.ApplyRecruitmentDTO;
import com.competition.backend.dto.AuditApplyDTO;
import com.competition.backend.dto.CreateTeacherRecruitmentDTO;

public interface RecruitmentService {

    void createTeacherRecruitment(
            Long teacherId,
            CreateTeacherRecruitmentDTO dto);

    void closeTeacherRecruitment(
            Long teacherId,
            Long recruitmentId);

    void applyTeacherRecruitment(
            Long studentId,
            Long recruitmentId,
            ApplyRecruitmentDTO dto);

    void auditApply(
            Long teacherId,
            Long applyId,
            AuditApplyDTO dto);
}