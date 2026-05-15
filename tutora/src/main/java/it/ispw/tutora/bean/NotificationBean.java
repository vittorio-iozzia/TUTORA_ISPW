package it.ispw.tutora.bean;

import it.ispw.tutora.model.Notification;
import java.util.List;

public class NotificationBean {
    private List<Notification> list;
    private int unreadCount;
    private int notificationId;
    private String errorMessage;

    public List<Notification> getList() { return list; }
    public void setList(List<Notification> list) { this.list = list; }
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
