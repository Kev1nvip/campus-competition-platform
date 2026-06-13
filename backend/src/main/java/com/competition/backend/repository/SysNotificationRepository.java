package com.competition.backend.repository;

import com.competition.backend.entity.SysNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SysNotificationRepository extends JpaRepository<SysNotification, Long> {

    Page<SysNotification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    long countByReceiverIdAndIsRead(Long receiverId, Boolean isRead);

    @Modifying
    @Query("UPDATE SysNotification n SET n.isRead = true WHERE n.receiverId = :receiverId AND n.isRead = false")
    int markAllRead(Long receiverId);
}
