package com.prevpaper.comman.dto;

//import com.prevpaper.comman.enums.NotificationEventType;
//import com.prevpaper.comman.enums.NotificationType;
//import lombok.Builder;
//import lombok.Data;
//
//import java.util.List;
//import java.util.UUID;
//
//
//import com.fasterxml.jackson.annotation.JsonCreator;
//import com.fasterxml.jackson.annotation.JsonProperty;
//
//@Builder
//@Data
//public class CommonNotificationRequest {
//    private UUID userId;
//    private String recipient;
//    private String title;
//    private String message;
//    private NotificationEventType eventType;
//    private List<NotificationType> notificationTypes;
//
//    @JsonCreator
//    public CommonNotificationRequest(
//            @JsonProperty("userId") UUID userId,
//            @JsonProperty("recipient") String recipient,
//            @JsonProperty("title") String title,
//            @JsonProperty("message") String message,
//            @JsonProperty("eventType") NotificationEventType eventType,
//            @JsonProperty("notificationTypes") List<NotificationType> notificationTypes
//    ) {
//        this.userId = userId;
//        this.recipient = recipient;
//        this.title = title;
//        this.message = message;
//        this.eventType = eventType;
//        this.notificationTypes = notificationTypes;
//    }
//}


import com.prevpaper.comman.enums.NotificationEventType;
import com.prevpaper.comman.enums.NotificationType;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor // Required by Jackson for deserialization
@AllArgsConstructor // Required by Lombok's @Builder
public class CommonNotificationRequest {
    private UUID userId;
    private String recipient;
    private String title;
    private String message;
    private NotificationEventType eventType;
    private List<NotificationType> notificationTypes;
}