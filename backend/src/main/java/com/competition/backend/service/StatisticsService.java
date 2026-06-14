package com.competition.backend.service;

import com.competition.backend.vo.StatisticsVO;

public interface StatisticsService {
    // 获取综合数据看板视图
    StatisticsVO getDashboardStatistics();
}