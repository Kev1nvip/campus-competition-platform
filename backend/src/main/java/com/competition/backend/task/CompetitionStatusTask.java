package com.competition.backend.task;

import com.competition.backend.entity.Competition;
import com.competition.backend.repository.CompetitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionStatusTask {

    private final CompetitionRepository competitionRepository;

    /**
     * 每分钟扫描一次，根据当前时间自动更新竞赛状态
     * UPCOMING → SIGNING → CLOSED → ONGOING → FINISHED
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional(rollbackFor = Exception.class)
    public void updateCompetitionStatus() {
        OffsetDateTime now = OffsetDateTime.now();

        // 查询所有未下架的竞赛（OFFLINE 不参与自动流转）
        List<Competition> competitions = competitionRepository.findAllByStatusNot("OFFLINE");

        int updated = 0;
        for (Competition c : competitions) {
            String newStatus = calcStatus(c, now);
            if (newStatus != null && !newStatus.equals(c.getStatus())) {
                c.setStatus(newStatus);
                competitionRepository.save(c);
                updated++;
                log.debug("竞赛[{}] 状态 {} → {}", c.getId(), c.getStatus(), newStatus);
            }
        }

        if (updated > 0) {
            log.info("竞赛状态自动流转完成，本次更新 {} 条", updated);
        }
    }

    private String calcStatus(Competition c, OffsetDateTime now) {
        // 已结束不再流转
        if ("FINISHED".equals(c.getStatus())) return null;

        if (now.isBefore(c.getSignupStart())) return "UPCOMING";
        if (now.isBefore(c.getSignupEnd()))   return "SIGNING";

        // 报名已截止
        if (c.getCompetitionStart() != null && now.isBefore(c.getCompetitionStart())) return "CLOSED";
        if (c.getCompetitionEnd()   != null && now.isBefore(c.getCompetitionEnd()))   return "ONGOING";

        // competitionEnd 已过，或未配置比赛时间则报名截止即结束
        return "FINISHED";
    }
}
