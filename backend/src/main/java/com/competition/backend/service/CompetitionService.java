package com.competition.backend.service;

import com.competition.backend.common.result.PageVO;
import com.competition.backend.dto.CompetitionSaveDTO;
import com.competition.backend.entity.Competition;
import com.competition.backend.vo.CompetitionListVO;

import java.time.OffsetDateTime;

public interface CompetitionService {
    
    // 发布竞赛
    Long createCompetition(CompetitionSaveDTO saveDTO);

    // 分页查询列表
    PageVO<CompetitionListVO> getCompetitionList(
            int page, int size, String status, String type, String keyword);

    // 获取详情
    Competition getCompetitionDetail(Long id);

    // 编辑竞赛
    void updateCompetition(Long id, CompetitionSaveDTO saveDTO);

    // 变更状态 (下架/恢复)
    void changeStatus(Long id, String action);
}