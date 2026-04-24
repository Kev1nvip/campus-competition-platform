package com.competition.backend.service;

import com.competition.backend.common.constant.LuaScripts;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 缓存 Key 前缀
    private static final String COMP_QUOTA_KEY = "comp:quota:";
    private static final String TEACHER_QUOTA_KEY = "teacher:quota:";

    /**
     * 初始化竞赛名额
     */
    public void initCompetitionQuota(Long compId, Integer quota) {
        redisTemplate.opsForValue().set(COMP_QUOTA_KEY + compId, quota, 7, TimeUnit.DAYS);
    }

    /**
     * 原子扣减名额
     */
    public Long decrCompetitionQuota(Long compId, Integer amount) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LuaScripts.DECR_QUOTA, Long.class);
        return redisTemplate.execute(script, Collections.singletonList(COMP_QUOTA_KEY + compId), amount);
    }

    /**
     * 增加名额（用于报名取消或回滚）
     */
    public void incrCompetitionQuota(Long compId, Integer amount) {
        redisTemplate.opsForValue().increment(COMP_QUOTA_KEY + compId, amount);
    }

    /**
     * 原子增加老师带队数并校验上限
     */
    public Long incrTeacherCount(Long compId, Long teacherId, Integer limit) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LuaScripts.INCR_TEACHER_COUNT, Long.class);
        String key = TEACHER_QUOTA_KEY + compId + ":" + teacherId;
        return redisTemplate.execute(script, Collections.singletonList(key), limit);
    }

    /**
     * 减少老师带队计数（用于报名取消或回滚）
     */
    public void decrTeacherCount(Long compId, Long teacherId) {
    String key = TEACHER_QUOTA_KEY + compId + ":" + teacherId;
    redisTemplate.opsForValue().decrement(key);
    }
}