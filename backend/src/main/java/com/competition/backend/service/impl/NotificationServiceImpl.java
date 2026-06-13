package com.competition.backend.service.impl;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.result.PageVO;
import com.competition.backend.entity.SysNotification;
import com.competition.backend.repository.SysNotificationRepository;
import com.competition.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SysNotificationRepository notificationRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(Long receiverId, String type, String title, String content, Long relatedId) {
        SysNotification n = SysNotification.builder()
                .receiverId(receiverId)
                .type(type)
                .title(title)
                .content(content)
                .relatedId(relatedId)
                .isRead(false)
                .build();
        notificationRepository.save(n);
    }

    @Override
    public PageVO<SysNotification> getMyNotifications(Long userId, int page, int size) {
        Page<SysNotification> p = notificationRepository
                .findByReceiverIdOrderByCreatedAtDesc(userId, PageRequest.of(page - 1, size));
        return PageVO.of(p);
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByReceiverIdAndIsRead(userId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long notificationId) {
        SysNotification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "通知不存在"));
        if (!n.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作");
        }
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }
}
