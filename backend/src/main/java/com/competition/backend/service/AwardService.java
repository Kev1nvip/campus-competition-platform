package com.competition.backend.service;

import com.competition.backend.dto.AwardAuditDTO;
import com.competition.backend.dto.CreateAwardDTO;
import com.competition.backend.vo.AwardVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AwardService {
    void createAward(Long userId, CreateAwardDTO dto);
    void auditAward(Long adminId, AwardAuditDTO dto);
    Page<AwardVO> getAwardsByCompetition(Long competitionId, Pageable pageable);
    Page<AwardVO> getMyAwards(Long userId, Pageable pageable);
}