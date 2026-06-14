package com.competition.backend.task;

import com.competition.backend.entity.Competition;
import com.competition.backend.repository.CompetitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSyncTask {

    private final CompetitionRepository competitionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 每 10 分钟同步一次 Redis 中的报名人数到数据库
     * 避免高频直接写库，同时保证数据最终一致
     */
    @Scheduled(fixedRate = 600000)
    public void syncEnrolledCount() {
        log.info("开始同步竞赛名额数据从 Redis 到 Database...");
        Set<String> keys = redisTemplate.keys("comp:quota:*");
        if (keys == null) return;

        for (String key : keys) {
            try {
                Long compId = Long.parseLong(key.replace("comp:quota:", ""));
                Object raw = redisTemplate.opsForValue().get(key);
                if (raw == null) continue;
                int remainingQuota = ((Number) raw).intValue();

                competitionRepository.findById(compId).ifPresent(comp -> {
                    if (comp.getMaxQuota() == null) return;
                    int enrolled = comp.getMaxQuota() - remainingQuota;
                    if (comp.getEnrolledCount() != enrolled) {
                        comp.setEnrolledCount(enrolled);
                        competitionRepository.save(comp);
                    }
                });
            } catch (Exception e) {
                log.error("同步 Key {} 出错", key, e);
            }
        }
    }
}