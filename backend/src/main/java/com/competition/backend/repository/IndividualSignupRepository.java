package com.competition.backend.repository;

import com.competition.backend.entity.IndividualSignup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IndividualSignupRepository extends JpaRepository<IndividualSignup, Long> {
    boolean existsByCompetitionIdAndStudentId(Long competitionId, Long studentId);
    Page<IndividualSignup> findByStudentId(Long studentId, Pageable pageable);
    Page<IndividualSignup> findByStudentIdAndStatus(Long studentId, String status, Pageable pageable);
}