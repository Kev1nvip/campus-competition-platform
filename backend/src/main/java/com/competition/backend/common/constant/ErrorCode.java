package com.competition.backend.common.constant;

public class ErrorCode {

    private ErrorCode() {}

    // ================================
    // 通用错误
    // ================================
    public static final int PARAM_ERROR       = 40000;
    public static final int PARAM_NULL        = 40001;
    public static final int PARAM_FORMAT      = 40002;
    public static final int UNAUTHORIZED      = 40100;
    public static final int TOKEN_INVALID     = 40101;
    public static final int FORBIDDEN         = 40300;
    public static final int NOT_FOUND         = 40400;
    public static final int CONFLICT          = 40900;
    public static final int SERVER_ERROR      = 50000;

    // ================================
    // 用户模块
    // ================================
    public static final int USER_PASSWORD_ERROR = 40101;
    public static final int USER_DISABLED       = 40102;
    public static final int USER_NAME_EXISTS    = 40103;
    public static final int STUDENT_NO_EXISTS   = 40104;
    public static final int USER_NOT_FOUND      = 40105;

    // ================================
    // 竞赛模块
    // ================================
    public static final int COMPETITION_NOT_FOUND    = 40120;
    public static final int COMPETITION_NOT_SIGNING  = 40121;
    public static final int COMPETITION_QUOTA_FULL   = 40122;
    public static final int COMPETITION_NO_PERMISSION = 40123;

    // ================================
    // 报名模块
    // ================================
    public static final int SIGNUP_DUPLICATE    = 40130;
    public static final int SIGNUP_NOT_FOUND    = 40131;
    public static final int SIGNUP_STATUS_ERROR = 40132;
    public static final int TEACHER_QUOTA_FULL  = 40133;

    // ================================
    // 队伍模块
    // ================================
    public static final int TEAM_NOT_FOUND            = 40140;
    public static final int TEAM_ALREADY_JOINED       = 40141;
    public static final int TEAM_MEMBER_FULL          = 40142;
    public static final int TEAM_NO_LEADER            = 40143;
    public static final int TEAM_SUBMITTED            = 40144;
    public static final int TEAM_TEACHER_NOT_CONFIRMED = 40145;

    // ================================
    // 申请模块
    // ================================
    public static final int APPLY_NOT_FOUND       = 40150;
    public static final int APPLY_DUPLICATE       = 40151;
    public static final int APPLY_ALREADY_HANDLED = 40152;

    // ================================
    // 审核模块
    // ================================
    public static final int AUDIT_NOT_FOUND  = 40160;
    public static final int AUDIT_NOT_NEEDED = 40161;

    // ================================
    // 获奖模块
    // ================================
    public static final int AWARD_NOT_FOUND            = 40170;
    public static final int AWARD_DUPLICATE            = 40171;
    public static final int AWARD_SIGNUP_NOT_APPROVED  = 40172;
}