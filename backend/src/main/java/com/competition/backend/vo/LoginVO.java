package com.competition.backend.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {

    private String token;
    private String tokenType;
    private Long expiresIn;
    private UserInfo userInfo;

    @Data
    @Builder
    public static class UserInfo {
        private Long userId;
        private String username;
        private String realName;
        private String role;
        private String department;
        private String avatarUrl;
    }
}