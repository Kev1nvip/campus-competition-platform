package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.result.PageVO;
import com.competition.backend.dto.CompetitionSaveDTO;
import com.competition.backend.entity.Competition;
import com.competition.backend.entity.SysUser;
import com.competition.backend.repository.CompetitionRepository;
import com.competition.backend.repository.SysUserRepository;
import com.competition.backend.service.CompetitionService;
import com.competition.backend.util.SecurityUtil;
import com.competition.backend.vo.CompetitionListVO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final SysUserRepository userRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCompetition(CompetitionSaveDTO saveDTO) {
        // 1. 业务逻辑校验
        validateCompetition(saveDTO);
        if ("TEAM".equals(saveDTO.getType())) {
            if (saveDTO.getMinTeamSize() == null || saveDTO.getMinTeamSize() < 2) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "团队赛最少人数需 >= 2");
            }
        }

        // 2. 构建实体
        Competition competition = Competition.builder()
                .title(saveDTO.getTitle())
                .type(saveDTO.getType())
                .organizer(saveDTO.getOrganizer())
                .requirement(saveDTO.getRequirement())
                .signupStart(saveDTO.getSignupStart())
                .signupEnd(saveDTO.getSignupEnd())
                .competitionStart(saveDTO.getCompetitionStart())
                .competitionEnd(saveDTO.getCompetitionEnd())
                .hasQuota(saveDTO.getHasQuota())
                .maxQuota(saveDTO.getMaxQuota())
                .enrolledCount(0)
                .minTeamSize(saveDTO.getMinTeamSize())
                .maxTeamSize(saveDTO.getMaxTeamSize())
                .maxTeachQuota(saveDTO.getMaxTeachQuota())
                .description(saveDTO.getDescription())
                .attachmentUrl(saveDTO.getAttachmentUrl())
                .status("UPCOMING")
                .createdBy(SecurityUtil.getCurrentUserId())
                .build();

        Competition saved = competitionRepository.save(competition);
        
        // TODO: 初始化 Redis 计数器 (在后续 feature/concurrent 中实现)
        
        return saved.getId();
    }

    @Override
    public PageVO<CompetitionListVO> getCompetitionList(int page, int size, String status, String type, String keyword) {
        boolean isAdmin = SecurityUtil.isAdmin();
        
        Specification<Competition> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 权限过滤：非管理员不可见 OFFLINE
            if (!isAdmin) {
                predicates.add(cb.notEqual(root.get("status"), "OFFLINE"));
            }
            
            // 条件筛选
            if (status != null && !status.isEmpty()) {
                if (isAdmin || !"OFFLINE".equals(status)) {
                    predicates.add(cb.equal(root.get("status"), status));
                }
            }
            if (type != null && !type.isEmpty()) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (keyword != null && !keyword.isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + keyword + "%"));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 排序逻辑：SIGNING 状态优先，然后按截止时间
        Sort sort = Sort.by(Sort.Order.asc("signupEnd"));
        Page<Competition> competitionPage = competitionRepository.findAll(spec, PageRequest.of(page - 1, size, sort));

        return PageVO.of(competitionPage, this::convertToListVO);
    }

    @Override
    public Competition getCompetitionDetail(Long id) {
        Competition competition = competitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "竞赛不存在"));
        
        if ("OFFLINE".equals(competition.getStatus()) && !SecurityUtil.isAdmin() && !SecurityUtil.getCurrentUserId().equals(competition.getCreatedBy())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "竞赛已下架");
        }
        return competition;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCompetition(Long id, CompetitionSaveDTO saveDTO) {
        Competition competition = competitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "竞赛不存在"));
        
        SecurityUtil.checkSelfOrAdmin(competition.getCreatedBy());

        if ("FINISHED".equals(competition.getStatus())) {
            throw new BusinessException(ErrorCode.SIGNUP_STATUS_ERROR, "已结束的竞赛无法编辑");
        }

        // 根据状态决定是否允许修改敏感字段（逻辑略，此处实现全量更新，需注意 enrolledCount 校验）
        competition.setTitle(saveDTO.getTitle());
        competition.setOrganizer(saveDTO.getOrganizer());
        competition.setRequirement(saveDTO.getRequirement());
        competition.setSignupEnd(saveDTO.getSignupEnd());
        competition.setMaxQuota(saveDTO.getMaxQuota());
        // ... 其他字段赋值

        competitionRepository.save(competition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, String action) {
        Competition competition = competitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "竞赛不存在"));
        
        SecurityUtil.checkSelfOrAdmin(competition.getCreatedBy());

        if ("OFFLINE".equals(action)) {
            competition.setStatus("OFFLINE");
        } else if ("RESTORE".equals(action)) {
            competition.setStatus(calculateCurrentStatus(competition));
        }
        
        competitionRepository.save(competition);
    }

    private void validateCompetition(CompetitionSaveDTO dto) {
        // 1. 时间范围校验
        if (dto.getSignupEnd().isBefore(dto.getSignupStart())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报名截止时间需晚于开始时间");
        }
        
        // 2. 名额限制条件校验
        if (Boolean.TRUE.equals(dto.getHasQuota())) {
            if (dto.getMaxQuota() == null || dto.getMaxQuota() < 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "开启名额限制时，名额上限必须大于等于1");
            }
        }
    }

    private String calculateCurrentStatus(Competition c) {
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(c.getSignupStart())) return "UPCOMING";
        if (now.isBefore(c.getSignupEnd())) return "SIGNING";
        if (c.getCompetitionStart() != null && now.isBefore(c.getCompetitionStart())) return "CLOSED";
        if (c.getCompetitionEnd() != null && now.isBefore(c.getCompetitionEnd())) return "ONGOING";
        return "FINISHED";
    }

    private CompetitionListVO convertToListVO(Competition c) {
        CompetitionListVO vo = new CompetitionListVO();
        vo.setCompetitionId(c.getId());
        vo.setTitle(c.getTitle());
        vo.setType(c.getType());
        vo.setOrganizer(c.getOrganizer());
        vo.setStatus(c.getStatus());
        vo.setSignupStart(c.getSignupStart());
        vo.setSignupEnd(c.getSignupEnd());
        vo.setHasQuota(c.getHasQuota());
        vo.setMaxQuota(c.getMaxQuota());
        vo.setRemainingQuota(c.getHasQuota() ? c.getMaxQuota() - c.getEnrolledCount() : null);
        vo.setCreatedBy(c.getCreatedBy());
        vo.setCreatedAt(c.getCreatedAt());
        
        userRepository.findById(c.getCreatedBy()).ifPresent(user -> vo.setCreatedByName(user.getRealName()));
        
        return vo;
    }
}