package com.competition.backend.service;

import com.competition.backend.entity.SysNotification;
import com.competition.backend.common.result.PageVO;

public interface NotificationService {

    /** 发送通知（内部调用） */
    void send(Long receiverId, String type, String title, String content, Long relatedId);

    /** 查询当前用户的通知列表 */
    PageVO<SysNotification> getMyNotifications(Long userId, int page, int size);

    /** 当前用户未读数 */
    long countUnread(Long userId);

    /** 标记单条已读 */
    void markRead(Long userId, Long notificationId);

    /** 全部标记已读 */
    void markAllRead(Long userId);
}
