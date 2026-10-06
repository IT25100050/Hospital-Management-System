package com.hospital.hms.notification.service;

import com.hospital.hms.notification.model.Notification;
import java.util.List;

public interface NotificationService {
    void sendNotification(Long userId, String message);
    List<Notification> getUserNotifications(Long userId);
    void markAsRead(Long notificationId);
}
