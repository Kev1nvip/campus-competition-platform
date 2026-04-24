package com.competition.backend.common.constant;

public class LuaScripts {
    
    /**
     * 竞赛名额扣减脚本
     * 参数：KEYS[1] 竞赛名额Key, ARGV[1] 扣减数量
     * 返回值：剩余名额数量；-1表示Key不存在；-2表示名额不足
     */
    public static final String DECR_QUOTA = 
        "local key = KEYS[1] " +
        "local amount = tonumber(ARGV[1]) " +
        "local current = redis.call('get', key) " +
        "if not current then return -1 end " +
        "if tonumber(current) >= amount then " +
            "return redis.call('decrby', key, amount) " +
        "else " +
            "return -2 " +
        "end";

    /**
     * 老师带队数量增加脚本
     * 参数：KEYS[1] 老师带队计数Key, ARGV[1] 限制数量
     * 返回值：增加后的数量；-1表示超过限制
     */
    public static final String INCR_TEACHER_COUNT = 
        "local key = KEYS[1] " +
        "local limit = tonumber(ARGV[1]) " +
        "local current = tonumber(redis.call('get', key) or '0') " +
        "if current < limit then " +
            "return redis.call('incr', key) " +
        "else " +
            "return -1 " +
        "end";
}