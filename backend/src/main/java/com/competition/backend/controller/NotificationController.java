package com.competition.backend.controller;

import com.competition.backend.common.result.PageVO;
import com.competition.backend.common.result.Result;
import com.competition.backend.entity.SysNotification;
import com.competition.backend.service.NotificationService;
import com.competition.backend.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "消息通知模块")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "获取我的通知列表")
    @GetMapping
    public Result<PageVO<SysNotification>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(notificationService.getMyNotifications(userId, page, size));
    }

    @Operation(summary = "获取未读数量")
    @GetMapping("/unread/count")
    public Result<Long> unreadCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(notificationService.countUnread(userId));
    }

    @Operation(summary = "标记单条已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.markRead(userId, id);
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read/all")
    public Result<Map<String, Integer>> markAllRead() {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.markAllRead(userId);
        return Result.success(Map.of("updated", 1));
    }
}
