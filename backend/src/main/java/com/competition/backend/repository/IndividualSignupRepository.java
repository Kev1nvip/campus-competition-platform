package com.competition.backend.repository;

import com.competition.backend.entity.IndividualSignup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IndividualSignupRepository extends JpaRepository<IndividualSignup, Long> {
    boolean existsByCompetitionIdAndStudentId(Long competitionId, Long studentId);
    Page<IndividualSignup> findByStudentId(Long studentId, Pageable pageable);
    Page<IndividualSignup> findByStudentIdAndStatus(Long studentId, String status, Pageable pageable);
    List<IndividualSignup> findByTeacherIdAndStatus(Long teacherId, String status);
    List<IndividualSignup> findByTeacherIdAndCompetitionIdAndStatus(Long teacherId, Long competitionId, String status);
    // 管理员：查询指定状态的报名（PENDING + RESUBMITTED）
    Page<IndividualSignup> findByStatusIn(java.util.List<String> statuses, Pageable pageable);
}