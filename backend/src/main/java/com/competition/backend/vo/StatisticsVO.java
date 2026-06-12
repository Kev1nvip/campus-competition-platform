package com.competition.backend.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatisticsVO {

    // 1. 用户统计
    private UserStats userStats;
    // 2. 竞赛统计
    private CompetitionStats competitionStats;
    // 3. 获奖统计
    private AwardStats awardStats;

    @Data
    @Builder
    public static class UserStats {
        private long totalUsers;    // 总用户数
        private long studentCount;  // 学生数
        private long teacherCount;  // 老师数
    }

    @Data
    @Builder
    public static class CompetitionStats {
        private long totalCompetitions; // 总竞赛数
        private long signingCount;      // 报名中的竞赛数
        private long judgingCount;      // 评审中的竞赛数
        private long endedCount;        // 已结束的竞赛数
    }

    @Data
    @Builder
    public static class AwardStats {
        private long totalAwards;     // 总申报获奖数
        private long approvedAwards;  // 已通过的获奖数
        private long pendingAwards;   // 待审核的获奖数
    }
}