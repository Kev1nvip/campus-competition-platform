package com.competition.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新个人信息请求")
public class UserUpdateDTO {

    @Schema(description = "真实姓名", example = "张三")
    @Size(max = 32, message = "姓名长度不能超过32位")
    private String realName;

    @Schema(description = "手机号", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "院系", example = "计算机学院")
    @Size(max = 64, message = "院系名称不能超过64位")
    private String department;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    @Size(max = 512, message = "头像URL不能超过512位")
    private String avatarUrl;

    @Schema(description = "职称（仅教师可填）", example = "副教授")
    @Size(max = 32, message = "职称不能超过32位")
    private String title;
}