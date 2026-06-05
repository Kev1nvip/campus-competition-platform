package com.competition.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Schema(description = "个人信息返回对象")
public class UserInfoVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "角色：STUDENT / TEACHER / ADMIN")
    private String role;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "学号（学生专属）")
    private String studentNo;

    @Schema(description = "院系")
    private String department;

    @Schema(description = "职称（教师专属）")
    private String title;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "账号状态：ACTIVE / DISABLED")
    private String status;

    @Schema(description = "注册时间")
    private OffsetDateTime createdAt;
}