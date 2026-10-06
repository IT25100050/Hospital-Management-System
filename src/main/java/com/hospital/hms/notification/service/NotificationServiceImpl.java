package com.hospital.hms.notification.service;

import com.hospital.hms.notification.model.Notification;
import com.hospital.hms.notification.service.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    // Simple in-memory demonstration list (Replace with NotificationRepository if database storage is needed)
    private final List<Notification> notificationStore = new ArrayList<>();

    @Override
    public void sendNotification(Long userId, String message) {
        Notification notification = new Notification(userId, message, false, LocalDateTime.now());
        notificationStore.add(notification);
    }

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        return notificationStore.stream()
                .filter(n -> n.getUserId().equals(userId))
                .toList();
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationStore.stream()
                .filter(n -> n.getId() != null && n.getId().equals(notificationId))
                .findFirst()
                .ifPresent(n -> n.setReadStatus(true));
    }
}
