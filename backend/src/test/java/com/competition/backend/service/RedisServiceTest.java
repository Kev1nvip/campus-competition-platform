package com.competition.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("第六部分 6.1 - Redis Lua 脚本语义测试（RedisService）")
@SuppressWarnings("unchecked")
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private RedisService redisService;

    @BeforeEach
    void setUp() {
        redisService = new RedisService(redisTemplate);
    }

    // ------------------------------------------------------------------ 6.1
    @Nested
    @DisplayName("decrCompetitionQuota - 竞赛名额扣减脚本")
    class DecrCompetitionQuota {

        @Test
        @DisplayName("6.1.1 名额充足 - 返回剩余数量 5")
        void decrQuota_sufficientQuota_returns5() {
            given(redisTemplate.execute(any(RedisScript.class), any(List.class), any()))
                    .willReturn(5L);

            Long result = redisService.decrCompetitionQuota(1L, 1);

            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("6.1.2 名额不足 - 返回 -2")
        void decrQuota_quotaExhausted_returnsNeg2() {
            given(redisTemplate.execute(any(RedisScript.class), any(List.class), any()))
                    .willReturn(-2L);

            Long result = redisService.decrCompetitionQuota(1L, 1);

            assertThat(result).isEqualTo(-2L);
        }

        @Test
        @DisplayName("6.1.3 缓存 Key 不存在 - 返回 -1")
        void decrQuota_keyMissing_returnsNeg1() {
            given(redisTemplate.execute(any(RedisScript.class), any(List.class), any()))
                    .willReturn(-1L);

            Long result = redisService.decrCompetitionQuota(1L, 1);

            assertThat(result).isEqualTo(-1L);
        }
    }

    // ------------------------------------------------------------------ 6.1
    @Nested
    @DisplayName("incrTeacherCount - 老师带队数量脚本")
    class IncrTeacherCount {

        @Test
        @DisplayName("6.1.4 老师带队未超限 - 返回当前带队数 1")
        void incrTeacher_belowLimit_returns1() {
            given(redisTemplate.execute(any(RedisScript.class), any(List.class), any()))
                    .willReturn(1L);

            Long result = redisService.incrTeacherCount(1L, 2L, 3);

            assertThat(result).isEqualTo(1L);
        }

        @Test
        @DisplayName("6.1.5 老师带队已满 - 返回 -1")
        void incrTeacher_atLimit_returnsNeg1() {
            given(redisTemplate.execute(any(RedisScript.class), any(List.class), any()))
                    .willReturn(-1L);

            Long result = redisService.incrTeacherCount(1L, 2L, 3);

            assertThat(result).isEqualTo(-1L);
        }
    }
}
