package com.competition.backend.service.impl;

import com.competition.backend.repository.AwardRecordRepository;
import com.competition.backend.repository.CompetitionRepository;
import com.competition.backend.repository.SysUserRepository;
import com.competition.backend.service.StatisticsService;
import com.competition.backend.vo.StatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SysUserRepository sysUserRepository;
    private final CompetitionRepository competitionRepository;
    private final AwardRecordRepository awardRecordRepository;

    @Override
    public StatisticsVO getDashboardStatistics() {

        // 1. 组装用户统计
        StatisticsVO.UserStats userStats = StatisticsVO.UserStats.builder()
                .totalUsers(sysUserRepository.count())
                .studentCount(sysUserRepository.countByRole("STUDENT"))
                .teacherCount(sysUserRepository.countByRole("TEACHER"))
                .build();

        // 2. 组装竞赛统计
        StatisticsVO.CompetitionStats compStats = StatisticsVO.CompetitionStats.builder()
                .totalCompetitions(competitionRepository.count())
                .signingCount(competitionRepository.countByStatus("SIGNING"))
                .judgingCount(competitionRepository.countByStatus("ONGOING"))
                .endedCount(competitionRepository.countByStatus("FINISHED"))
                .build();

        // 3. 组装获奖统计
        StatisticsVO.AwardStats awardStats = StatisticsVO.AwardStats.builder()
                .totalAwards(awardRecordRepository.count())
                .approvedAwards(awardRecordRepository.countByStatus("APPROVED"))
                .pendingAwards(awardRecordRepository.countByStatus("PENDING"))
                .build();

        // 4. 返回大包
        return StatisticsVO.builder()
                .userStats(userStats)
                .competitionStats(compStats)
                .awardStats(awardStats)
                .build();
    }
}