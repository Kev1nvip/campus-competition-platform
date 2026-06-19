package com.competition.backend.repository;

import com.competition.backend.entity.CompetitionDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionDocumentRepository extends JpaRepository<CompetitionDocument, Long> {
    List<CompetitionDocument> findByCompetitionId(Long competitionId);
    Optional<CompetitionDocument> findByIdAndCompetitionId(Long id, Long competitionId);
}
