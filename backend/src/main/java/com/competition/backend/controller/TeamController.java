package com.competition.backend.controller;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.result.Result;
import com.competition.backend.dto.CreateTeamDTO;
import com.competition.backend.entity.ApplyRecord;
import com.competition.backend.entity.SysUser;
import com.competition.backend.entity.Team;
import com.competition.backend.entity.TeamMember;
import com.competition.backend.repository.ApplyRecordRepository;
import com.competition.backend.repository.CompetitionRepository;
import com.competition.backend.repository.SysUserRepository;
import com.competition.backend.repository.TeamMemberRepository;
import com.competition.backend.repository.TeamRepository;
import com.competition.backend.service.NotificationService;
import com.competition.backend.service.TeamService;
import com.competition.backend.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "组队模块")
@RestController
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SysUserRepository userRepository;
    private final CompetitionRepository competitionRepository;
    private final ApplyRecordRepository applyRecordRepository;
    private final NotificationService notificationService;

    // ──────────────────────────────────────────────
    // 队伍大厅：所有 FORMING 状态的队伍
    // ──────────────────────────────────────────────
    @Operation(summary = "队伍大厅（所有招募中的队伍）")
    @GetMapping("/hall")
    public Result<List<Map<String, Object>>> teamHall(
            @RequestParam(required = false) Long competitionId) {
        List<Team> teams = teamRepository.findAll().stream()
                .filter(t -> "FORMING".equals(t.getStatus()))
                .filter(t -> competitionId == null || t.getCompetitionId().equals(competitionId))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = teams.stream().map(t -> buildTeamItem(t)).collect(Collectors.toList());
        return Result.success(result);
    }

    // ──────────────────────────────────────────────
    // 我的队伍：队长 + 队员身份的所有队伍
    // ──────────────────────────────────────────────
    @Operation(summary = "我的队伍（队长或队员）")
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> myTeams() {
        Long userId = SecurityUtil.getCurrentUserId();

        // 作为队长的队伍
        Set<Long> teamIds = new LinkedHashSet<>();
        teamRepository.findByLeaderId(userId).forEach(t -> teamIds.add(t.getId()));

        // 作为队员的队伍
        teamMemberRepository.findByStudentId(userId).forEach(m -> teamIds.add(m.getTeamId()));

        List<Map<String, Object>> result = teamIds.stream()
                .map(id -> teamRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(this::buildTeamItem)
                .collect(Collectors.toList());

        return Result.success(result);
    }

    // ──────────────────────────────────────────────
    // 队伍详情
    // ──────────────────────────────────────────────
    @Operation(summary = "队伍详情（含成员列表）")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> teamDetail(@PathVariable Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND, "队伍不存在"));

        Map<String, Object> detail = buildTeamItem(team);

        // 指导老师信息
        if (team.getTeacherId() != null) {
            userRepository.findById(team.getTeacherId()).ifPresent(u -> {
                detail.put("teacherName", u.getRealName());
                detail.put("teacherTitle", u.getTitle());
            });
        }

        // 成员列表
        List<Map<String, Object>> members = teamMemberRepository.findByTeamId(id).stream().map(m -> {
            Map<String, Object> member = new HashMap<>();
            member.put("studentId", m.getStudentId());
            member.put("role", m.getRole());
            member.put("joinedAt", m.getJoinedAt());
            userRepository.findById(m.getStudentId()).ifPresent(u -> {
                member.put("realName", u.getRealName());
                member.put("studentNo", u.getStudentNo());
                member.put("department", u.getDepartment());
            });
            return member;
        }).collect(Collectors.toList());
        detail.put("members", members);

        // 当前请求用户在该队伍中的角色
        Long currentUserId = SecurityUtil.getCurrentUserId();
        String currentUserRole = members.stream()
                .filter(m -> currentUserId.equals(m.get("studentId")))
                .findFirst()
                .map(m -> (String) m.get("role"))
                .orElse("NONE");

        // 兼容旧数据：team_member 表缺少队长记录时，通过 team.leaderId 判断
        if ("NONE".equals(currentUserRole) && currentUserId.equals(team.getLeaderId())) {
            currentUserRole = "LEADER";
        }
        detail.put("currentUserRole", currentUserRole);

        return Result.success(detail);
    }

    // ──────────────────────────────────────────────
    // 我的待处理邀请列表
    @Operation(summary = "我的待处理队伍邀请列表")
    @GetMapping("/invites/pending")
    public Result<List<Map<String, Object>>> myPendingInvites() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Map<String, Object>> result = applyRecordRepository.findByReceiverId(userId).stream()
                .filter(r -> "TEAM_INVITE".equals(r.getType()) && "PENDING".equals(r.getStatus()))
                .map(r -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("applyId", r.getId());
                    item.put("teamId", r.getBizId());
                    item.put("applicantId", r.getApplicantId());
                    item.put("createdAt", r.getCreatedAt());
                    teamRepository.findById(r.getBizId()).ifPresent(t -> {
                        item.put("teamName", t.getTeamName());
                        item.put("memberCount", t.getMemberCount());
                        competitionRepository.findById(t.getCompetitionId())
                                .ifPresent(c -> item.put("competitionTitle", c.getTitle()));
                    });
                    userRepository.findById(r.getApplicantId())
                            .ifPresent(u -> item.put("leaderName", u.getRealName()));
                    return item;
                })
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @Operation(summary = "处理队伍邀请（APPROVED/REJECTED）")
    @PutMapping("/invites/{applyId}/handle")
    public Result<Void> handleTeamInvite(
            @PathVariable Long applyId,
            @RequestParam String action) {
        teamService.handleInvite(SecurityUtil.getCurrentUserId(), applyId, action);
        return Result.success();
    }

    @Operation(summary = "学生申请加入队伍（向队长发申请）")
    @PostMapping("/{id}/apply")
    public Result<Void> applyJoin(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        Long applicantId = SecurityUtil.getCurrentUserId();
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND, "队伍不存在"));

        // 不能申请加入自己创建的队伍
        if (team.getLeaderId().equals(applicantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能申请加入自己的队伍");
        }
        if (!"FORMING".equals(team.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该队伍不在招募中");
        }
        if (teamMemberRepository.existsByTeamIdAndStudentId(id, applicantId)) {
            throw new BusinessException(ErrorCode.APPLY_DUPLICATE, "你已在该队伍中");
        }
        boolean hasPending = applyRecordRepository.findByReceiverId(team.getLeaderId()).stream()
                .anyMatch(r -> "TEAM_RECRUIT_APPLY".equals(r.getType())
                        && r.getBizId().equals(id)
                        && r.getApplicantId().equals(applicantId)
                        && "PENDING".equals(r.getStatus()));
        if (hasPending) {
            throw new BusinessException(ErrorCode.APPLY_DUPLICATE, "已向该队伍发送过申请，请等待队长处理");
        }

        ApplyRecord apply = ApplyRecord.builder()
                .type("TEAM_RECRUIT_APPLY")
                .applicantId(applicantId)
                .receiverId(team.getLeaderId())
                .bizId(id)
                .motivation(body != null ? body.get("motivation") : null)
                .status("PENDING")
                .build();
        ApplyRecord savedApply = applyRecordRepository.save(apply);

        // 通知队长，relatedId 存 applyId 方便前端直接操作
        String applicantName = userRepository.findById(applicantId)
                .map(u -> u.getRealName()).orElse("一名同学");
        notificationService.send(
                team.getLeaderId(),
                "APPLY_RECEIVED",
                "收到入队申请",
                "「" + applicantName + "」申请加入队伍「" + team.getTeamName() + "」",
                savedApply.getId()
        );
        return Result.success();
    }

    // 搜索学生（邀请队友用，按学号或姓名）
    // ──────────────────────────────────────────────
    @Operation(summary = "搜索学生（邀请队友用）")
    @GetMapping("/search-student")
    public Result<List<Map<String, Object>>> searchStudent(
            @RequestParam String keyword) {
        PageRequest page = PageRequest.of(0, 10);
        Set<SysUser> found = new LinkedHashSet<>();
        // 先按学号搜索
        userRepository.findByRoleAndStudentNoContaining("STUDENT", keyword, page).forEach(found::add);
        // 再按姓名搜索
        if (found.size() < 10) {
            userRepository.findByRoleAndRealNameContaining("STUDENT", keyword, page).forEach(found::add);
        }
        List<Map<String, Object>> result = found.stream().map(u -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("realName", u.getRealName());
            item.put("studentNo", u.getStudentNo());
            item.put("department", u.getDepartment());
            return item;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    // ──────────────────────────────────────────────
    // 创建队伍
    // ──────────────────────────────────────────────
    @Operation(summary = "创建队伍")
    @PostMapping
    public Result<Void> createTeam(@RequestBody CreateTeamDTO dto) {
        teamService.createTeam(SecurityUtil.getCurrentUserId(), dto);
        return Result.success();
    }

    @Operation(summary = "邀请队友")
    @PostMapping("/{id}/invite")
    public Result<Void> invite(@PathVariable Long id,
                               @RequestParam Long targetUserId) {
        teamService.inviteMember(SecurityUtil.getCurrentUserId(), id, targetUserId);
        return Result.success();
    }

    @Operation(summary = "处理邀请（接受/拒绝）")
    @PutMapping("/invite/{applyId}")
    public Result<Void> handleInvite(@PathVariable Long applyId,
                                     @RequestParam String status) {
        teamService.handleInvite(SecurityUtil.getCurrentUserId(), applyId, status);
        return Result.success();
    }

    @Operation(summary = "退出队伍")
    @DeleteMapping("/{id}/quit")
    public Result<Void> quit(@PathVariable Long id) {
        teamService.quitTeam(SecurityUtil.getCurrentUserId(), id);
        return Result.success();
    }

    // ──────────────────────────────────────────────
    // 带队申请：队长申请老师带队（TEAM_GUIDE）
    // ──────────────────────────────────────────────
    @Operation(summary = "队长申请老师带队")
    @PostMapping("/{id}/apply-teacher")
    public Result<Void> applyTeacher(
            @PathVariable Long id,
            @RequestParam Long teacherId) {
        Long leaderId = SecurityUtil.getCurrentUserId();
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND, "队伍不存在"));

        if (!team.getLeaderId().equals(leaderId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有队长可以申请带队");
        }
        if (Boolean.TRUE.equals(team.getTeacherConfirmed())) {
            throw new BusinessException(ErrorCode.APPLY_DUPLICATE, "已有老师确认带队");
        }

        // 检查是否有待处理的申请
        boolean hasPending = applyRecordRepository.findByReceiverId(teacherId).stream()
                .anyMatch(r -> "TEAM_GUIDE".equals(r.getType())
                        && r.getBizId().equals(id)
                        && "PENDING".equals(r.getStatus()));
        if (hasPending) {
            throw new BusinessException(ErrorCode.APPLY_DUPLICATE, "已向该老师发送过申请");
        }

        ApplyRecord apply = ApplyRecord.builder()
                .type("TEAM_GUIDE")
                .applicantId(leaderId)
                .receiverId(teacherId)
                .bizId(id)
                .status("PENDING")
                .build();
        ApplyRecord saved = applyRecordRepository.save(apply);

        // 通知老师
        String leaderName = userRepository.findById(leaderId).map(u -> u.getRealName()).orElse("队长");
        notificationService.send(teacherId, "APPLY_RECEIVED", "收到带队申请",
                "「" + leaderName + "」邀请你带领队伍「" + team.getTeamName() + "」参加竞赛",
                saved.getId());

        return Result.success();
    }

    @Operation(summary = "老师处理带队申请（同意/拒绝）")
    @PutMapping("/team-guide/{applyId}/handle")
    public Result<Void> handleTeamGuide(
            @PathVariable Long applyId,
            @RequestParam String action) {
        Long teacherId = SecurityUtil.getCurrentUserId();
        ApplyRecord apply = applyRecordRepository.findById(applyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLY_NOT_FOUND, "申请不存在"));

        if (!apply.getReceiverId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限处理");
        }
        if (!"PENDING".equals(apply.getStatus())) {
            throw new BusinessException(ErrorCode.APPLY_ALREADY_HANDLED, "已处理");
        }

        apply.setStatus(action);
        applyRecordRepository.save(apply);

        if ("APPROVED".equals(action)) {
            // 更新队伍：设置老师并标记已确认
            Team team = teamRepository.findById(apply.getBizId()).orElse(null);
            if (team != null) {
                team.setTeacherId(teacherId);
                team.setTeacherConfirmed(true);
                teamRepository.save(team);
                // 通知队长
                String teacherName = userRepository.findById(teacherId).map(u -> u.getRealName()).orElse("老师");
                notificationService.send(apply.getApplicantId(), "APPLY_APPROVED", "带队申请已通过",
                        "「" + teacherName + "」同意带领你的队伍「" + team.getTeamName() + "」，现在可以发布招募帖或提交报名",
                        team.getId());
            }
        } else if ("REJECTED".equals(action)) {
            Team team = teamRepository.findById(apply.getBizId()).orElse(null);
            if (team != null) {
                notificationService.send(apply.getApplicantId(), "APPLY_REJECTED", "带队申请被拒绝",
                        "你的带队申请被拒绝，请重新选择其他老师",
                        team.getId());
            }
        }
        return Result.success();
    }

    // ──────────────────────────────────────────────
    // 老师带队管理：查看已确认带队的队伍列表
    // ──────────────────────────────────────────────
    @Operation(summary = "老师查看带队队伍列表")
    @GetMapping("/teacher-team-list")
    public Result<List<Map<String, Object>>> teacherTeamList() {
        Long teacherId = SecurityUtil.getCurrentUserId();
        List<Team> teams = teamRepository.findByTeacherId(teacherId);
        List<Map<String, Object>> result = teams.stream().map(t -> {
            Map<String, Object> item = buildTeamItem(t);
            // 查询成员姓名
            List<String> memberNames = teamMemberRepository.findByTeamId(t.getId()).stream()
                    .map(m -> userRepository.findById(m.getStudentId())
                            .map(u -> u.getRealName()).orElse("未知"))
                    .collect(Collectors.toList());
            item.put("memberNames", memberNames);
            return item;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @Operation(summary = "老师查看待处理带队申请列表")
    @GetMapping("/team-guide/pending")
    public Result<List<Map<String, Object>>> pendingTeamGuides() {
        Long teacherId = SecurityUtil.getCurrentUserId();
        List<Map<String, Object>> result = applyRecordRepository.findByReceiverId(teacherId).stream()
                .filter(r -> "TEAM_GUIDE".equals(r.getType()) && "PENDING".equals(r.getStatus()))
                .map(r -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("applyId", r.getId());
                    item.put("teamId", r.getBizId());
                    item.put("applicantId", r.getApplicantId());
                    item.put("createdAt", r.getCreatedAt());
                    teamRepository.findById(r.getBizId()).ifPresent(t -> {
                        item.put("teamName", t.getTeamName());
                        item.put("memberCount", t.getMemberCount());
                        competitionRepository.findById(t.getCompetitionId())
                                .ifPresent(c -> item.put("competitionTitle", c.getTitle()));
                    });
                    userRepository.findById(r.getApplicantId())
                            .ifPresent(u -> item.put("leaderName", u.getRealName()));
                    return item;
                })
                .collect(Collectors.toList());
        return Result.success(result);
    }

    // ──────────────────────────────────────────────
    // 内部方法
    // ──────────────────────────────────────────────
    private Map<String, Object> buildTeamItem(Team t) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", t.getId());
        item.put("teamName", t.getTeamName());
        item.put("competitionId", t.getCompetitionId());
        item.put("leaderId", t.getLeaderId());
        item.put("status", t.getStatus());
        item.put("memberCount", t.getMemberCount());
        item.put("teacherConfirmed", t.getTeacherConfirmed());
        userRepository.findById(t.getLeaderId()).ifPresent(u -> item.put("leaderName", u.getRealName()));
        competitionRepository.findById(t.getCompetitionId()).ifPresent(c -> {
            item.put("competitionTitle", c.getTitle());
            item.put("competitionType", c.getType());
            item.put("minTeamSize", c.getMinTeamSize());
            item.put("maxTeamSize", c.getMaxTeamSize());
        });
        return item;
    }
}
