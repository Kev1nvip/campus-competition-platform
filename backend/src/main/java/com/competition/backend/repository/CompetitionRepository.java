package com.competition.backend.repository;

import com.competition.backend.entity.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long>, JpaSpecificationExecutor<Competition> {

    boolean existsByTitleAndTypeAndOrganizerAndCompetitionStartAndStatusNot(
            String title, String type, String organizer, OffsetDateTime competitionStart, String status);

    long countByStatus(String status);

    List<Competition> findAllByStatusNot(String status);
}