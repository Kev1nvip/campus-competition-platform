package com.competition.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "教师主页信息返回对象")
public class TeacherProfileVO {

    @Schema(description = "教师用户ID")
    private Long id;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "院系")
    private String department;

    @Schema(description = "职称")
    private String title;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像URL")
    private String avatarUrl;
}