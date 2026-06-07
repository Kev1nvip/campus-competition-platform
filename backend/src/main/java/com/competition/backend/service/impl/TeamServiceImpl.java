package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.dto.CreateTeamDTO;
import com.competition.backend.entity.ApplyRecord;
import com.competition.backend.entity.Team;
import com.competition.backend.repository.ApplyRecordRepository;
import com.competition.backend.repository.TeamRepository;
import com.competition.backend.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final ApplyRecordRepository applyRecordRepository;

    // ✅ 创建队伍
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTeam(Long userId, CreateTeamDTO dto) {

        Team team = Team.builder()
                .competitionId(dto.getCompetitionId())
                .teamName(dto.getTeamName())
                .leaderId(userId)
                .teacherConfirmed(false)
                .memberCount(1)
                .status("FORMING")
                .build();

        teamRepository.save(team);
    }

    // ✅ 邀请队友
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteMember(Long leaderId, Long teamId, Long targetUserId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TEAM_NOT_FOUND, "队伍不存在"));

        if (!team.getLeaderId().equals(leaderId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN, "只有队长可以邀请");
        }

        ApplyRecord apply = ApplyRecord.builder()
                .type("TEAM_INVITE")
                .applicantId(leaderId)
                .receiverId(targetUserId)
                .bizId(teamId)
                .status("PENDING")
                .build();

        applyRecordRepository.save(apply);
    }

    // ✅ 处理邀请
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleInvite(Long userId, Long applyId, String status) {

        ApplyRecord apply = applyRecordRepository.findById(applyId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.APPLY_NOT_FOUND, "申请不存在"));

        if (!apply.getReceiverId().equals(userId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN, "无权限处理");
        }

        if (!"PENDING".equals(apply.getStatus())) {
            throw new BusinessException(
                    ErrorCode.APPLY_ALREADY_HANDLED, "已处理");
        }

        if ("APPROVED".equals(status)) {

            Team team = teamRepository.findById(apply.getBizId())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.TEAM_NOT_FOUND, "队伍不存在"));

            team.setMemberCount(team.getMemberCount() + 1);

            teamRepository.save(team);
            apply.setStatus("APPROVED");

        } else if ("REJECTED".equals(status)) {

            apply.setStatus("REJECTED");

        } else {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR, "状态错误");
        }

        applyRecordRepository.save(apply);
    }

    // ✅ 退出队伍
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void quitTeam(Long userId, Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TEAM_NOT_FOUND, "队伍不存在"));

        if (team.getLeaderId().equals(userId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN, "队长不能直接退出");
        }

        team.setMemberCount(team.getMemberCount() - 1);
        teamRepository.save(team);
    }
}