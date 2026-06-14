package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.dto.CreateTeamDTO;
import com.competition.backend.entity.ApplyRecord;
import com.competition.backend.entity.Team;
import com.competition.backend.entity.TeamMember;
import com.competition.backend.repository.ApplyRecordRepository;
import com.competition.backend.repository.SysUserRepository;
import com.competition.backend.repository.TeamMemberRepository;
import com.competition.backend.repository.TeamRepository;
import com.competition.backend.service.NotificationService;
import com.competition.backend.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ApplyRecordRepository applyRecordRepository;
    private final SysUserRepository userRepository;
    private final NotificationService notificationService;

    // ────────────────────────────────────────────
    // 创建队伍
    // ────────────────────────────────────────────
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
        Team saved = teamRepository.save(team);

        // Bug1 修复：插入队长的 team_member 记录
        TeamMember leaderRecord = TeamMember.builder()
                .teamId(saved.getId())
                .studentId(userId)
                .role("LEADER")
                .joinedAt(OffsetDateTime.now())
                .build();
        teamMemberRepository.save(leaderRecord);
    }

    // ────────────────────────────────────────────
    // 邀请队友
    // ────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteMember(Long leaderId, Long teamId, Long targetUserId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND, "队伍不存在"));

        if (!team.getLeaderId().equals(leaderId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有队长可以邀请");
        }

        // 不能重复邀请已在队伍中的成员
        if (teamMemberRepository.existsByTeamIdAndStudentId(teamId, targetUserId)) {
            throw new BusinessException(ErrorCode.APPLY_DUPLICATE, "该同学已在队伍中");
        }

        // 不能对同一人重复发送待处理的邀请
        boolean hasPending = applyRecordRepository.findByReceiverId(targetUserId).stream()
                .anyMatch(r -> "TEAM_INVITE".equals(r.getType())
                        && r.getBizId().equals(teamId)
                        && "PENDING".equals(r.getStatus()));
        if (hasPending) {
            throw new BusinessException(ErrorCode.APPLY_DUPLICATE, "已向该同学发送过邀请，请等待回复");
        }

        ApplyRecord apply = ApplyRecord.builder()
                .type("TEAM_INVITE")
                .applicantId(leaderId)
                .receiverId(targetUserId)
                .bizId(teamId)
                .status("PENDING")
                .build();
        ApplyRecord savedApply = applyRecordRepository.save(apply);

        // 发送通知给被邀请人，relatedId 存 applyId 方便前端直接操作
        String leaderName = userRepository.findById(leaderId)
                .map(u -> u.getRealName()).orElse("队长");
        notificationService.send(
                targetUserId,
                "TEAM_INVITE",
                "收到队伍邀请",
                "「" + leaderName + "」邀请你加入队伍「" + team.getTeamName() + "」",
                savedApply.getId()
        );
    }

    // ────────────────────────────────────────────
    // 处理邀请（接受 / 拒绝）
    // ────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleInvite(Long userId, Long applyId, String status) {

        ApplyRecord apply = applyRecordRepository.findById(applyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLY_NOT_FOUND, "申请不存在"));

        if (!apply.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限处理");
        }

        if (!"PENDING".equals(apply.getStatus())) {
            throw new BusinessException(ErrorCode.APPLY_ALREADY_HANDLED, "已处理");
        }

        if ("APPROVED".equals(status)) {

            Team team = teamRepository.findById(apply.getBizId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND, "队伍不存在"));

            team.setMemberCount(team.getMemberCount() + 1);
            teamRepository.save(team);
            apply.setStatus("APPROVED");

            // 判断谁应该被加入队伍：
            // TEAM_INVITE：队长邀请队员，被邀请人（userId=receiverId）接受 → 加入该队员
            // TEAM_RECRUIT_APPLY：队员申请加入，队长（userId=receiverId）同意 → 加入申请人（applicantId）
            Long newMemberId = "TEAM_RECRUIT_APPLY".equals(apply.getType())
                    ? apply.getApplicantId()
                    : userId;

            TeamMember memberRecord = TeamMember.builder()
                    .teamId(team.getId())
                    .studentId(newMemberId)
                    .role("MEMBER")
                    .joinedAt(OffsetDateTime.now())
                    .build();
            teamMemberRepository.save(memberRecord);

            // 通知对方
            String memberName = userRepository.findById(newMemberId)
                    .map(u -> u.getRealName()).orElse("队员");
            if ("TEAM_RECRUIT_APPLY".equals(apply.getType())) {
                // 通知申请人：申请已通过
                notificationService.send(
                        apply.getApplicantId(),
                        "APPLY_APPROVED",
                        "入队申请已通过",
                        "你申请加入队伍「" + team.getTeamName() + "」已被队长同意，欢迎加入！",
                        team.getId()
                );
            } else {
                // 通知队长：成员已接受邀请
                notificationService.send(
                        team.getLeaderId(),
                        "APPLY_APPROVED",
                        "队友已接受邀请",
                        "「" + memberName + "」已接受你的邀请，加入队伍「" + team.getTeamName() + "」。",
                        team.getId()
                );
            }

        } else if ("REJECTED".equals(status)) {

            apply.setStatus("REJECTED");

            Team team = teamRepository.findById(apply.getBizId()).orElse(null);
            if (team != null) {
                if ("TEAM_RECRUIT_APPLY".equals(apply.getType())) {
                    // 通知申请人：被拒绝
                    notificationService.send(
                            apply.getApplicantId(),
                            "APPLY_REJECTED",
                            "入队申请被拒绝",
                            "你申请加入队伍「" + team.getTeamName() + "」的请求被队长拒绝。",
                            team.getId()
                    );
                } else {
                    // TEAM_INVITE：通知队长，被邀请人拒绝
                    String memberName = userRepository.findById(userId)
                            .map(u -> u.getRealName()).orElse("对方");
                    notificationService.send(
                            team.getLeaderId(),
                            "APPLY_REJECTED",
                            "队友拒绝了邀请",
                            "「" + memberName + "」拒绝了加入队伍「" + team.getTeamName() + "」的邀请。",
                            team.getId()
                    );
                }
            }

        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态错误");
        }

        applyRecordRepository.save(apply);
    }

    // ────────────────────────────────────────────
    // 退出队伍
    // ────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void quitTeam(Long userId, Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND, "队伍不存在"));

        if (team.getLeaderId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "队长不能直接退出，请解散队伍");
        }

        // 校验提交审核后不可退出
        if ("SUBMITTED".equals(team.getStatus()) || "APPROVED".equals(team.getStatus())) {
            throw new BusinessException(ErrorCode.TEAM_SUBMITTED, "队伍已提交审核，不可退出");
        }

        // Bug3 修复：删除 team_member 记录
        teamMemberRepository.findByTeamId(teamId).stream()
                .filter(m -> m.getStudentId().equals(userId))
                .findFirst()
                .ifPresent(teamMemberRepository::delete);

        team.setMemberCount(Math.max(1, team.getMemberCount() - 1));
        teamRepository.save(team);
    }
}
