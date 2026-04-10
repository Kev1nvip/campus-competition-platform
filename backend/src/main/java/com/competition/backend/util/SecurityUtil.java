package com.competition.backend.util;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.security.SecurityUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    private SecurityUtil() {}

    /**
     * 获取当前登录用户
     *
     * @return SecurityUser
     */
    public static SecurityUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或Token已过期");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof SecurityUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或Token已过期");
        }
        return (SecurityUser) principal;
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    /**
     * 获取当前登录用户角色
     *
     * @return 角色
     */
    public static String getCurrentUserRole() {
        return getCurrentUser().getRole();
    }

    /**
     * 判断当前用户是否是管理员
     *
     * @return true=是管理员
     */
    public static boolean isAdmin() {
        return "ADMIN".equals(getCurrentUserRole());
    }

    /**
     * 判断当前用户是否是老师
     *
     * @return true=是老师
     */
    public static boolean isTeacher() {
        return "TEACHER".equals(getCurrentUserRole());
    }

    /**
     * 判断当前用户是否是学生
     *
     * @return true=是学生
     */
    public static boolean isStudent() {
        return "STUDENT".equals(getCurrentUserRole());
    }

    /**
     * 校验当前用户是否是管理员，不是则抛出异常
     */
    public static void checkAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无操作权限");
        }
    }

    /**
     * 校验当前用户是否是指定用户，不是则抛出异常
     *
     * @param userId 目标用户ID
     */
    public static void checkSelf(Long userId) {
        if (!getCurrentUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无操作权限");
        }
    }

    /**
     * 校验当前用户是否是指定用户或管理员，都不是则抛出异常
     *
     * @param userId 目标用户ID
     */
    public static void checkSelfOrAdmin(Long userId) {
        if (!isAdmin() && !getCurrentUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无操作权限");
        }
    }
}