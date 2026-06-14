package com.competition.backend.task;

import com.competition.backend.config.RabbitMqConfig;
import com.competition.backend.dto.NotificationMessage;
import com.competition.backend.entity.SysNotification;
import com.competition.backend.repository.SysNotificationRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final SysNotificationRepository notificationRepository;

    @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(NotificationMessage msg,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            SysNotification n = SysNotification.builder()
                    .receiverId(msg.getReceiverId())
                    .type(msg.getType())
                    .title(msg.getTitle())
                    .content(msg.getContent())
                    .relatedId(msg.getRelatedId())
                    .isRead(false)
                    .build();
            notificationRepository.save(n);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("消息通知处理失败，receiverId={}, type={}", msg.getReceiverId(), msg.getType(), e);
            channel.basicNack(tag, false, false);
        }
    }
}
