package com.competition.backend.service;

import com.competition.backend.dto.CreateTeamDTO;

public interface TeamService {

    void createTeam(Long userId, CreateTeamDTO dto);

    void inviteMember(Long leaderId, Long teamId, Long targetUserId);

    void handleInvite(Long userId, Long applyId, String status);

    void quitTeam(Long userId, Long teamId);
}