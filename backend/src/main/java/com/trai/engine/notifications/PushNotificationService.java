package com.trai.engine.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PushNotificationService — Firebase Cloud Messaging (FCM) integration.
 *
 * Works for BOTH:
 *  - Android (via FCM data + notification messages)
 *  - iOS (via APNs bridge through FCM — no separate APNs code needed)
 *
 * Setup requirements:
 *  1. Create a Firebase project at https://console.firebase.google.com
 *  2. Add Android app → download google-services.json → place in mobile/android/app/
 *  3. Add iOS app → download GoogleService-Info.plist → place in mobile/ios/Runner/
 *  4. Generate Service Account key → download JSON → set FIREBASE_CREDENTIALS_PATH
 *
 * For iOS specifically:
 *  - Upload APNs certificate or APNs Auth Key in Firebase Console → Project Settings → Cloud Messaging
 *  - FCM handles the APNs relay automatically — no code change needed here
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    @Value("${firebase.credentials-path:}")
    private String credentialsPath;

    @Value("${firebase.server-key:}")
    private String serverKey;

    private boolean firebaseInitialized = false;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initializeFirebase() {
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            try {
                FileInputStream serviceAccount = new FileInputStream(credentialsPath);
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                }
                firebaseInitialized = true;
                log.info("[FCM] Firebase Admin SDK initialized from credentials file: {}", credentialsPath);
            } catch (IOException e) {
                log.warn("[FCM] Could not initialize Firebase from credentials path: {}. " +
                         "Push notifications will be logged only in dev mode.", e.getMessage());
            }
        } else {
            log.warn("[FCM] FIREBASE_CREDENTIALS_PATH not set. Push notifications will run in mock/log mode.");
        }
    }

    /**
     * Send a push notification to a single device token.
     * Works for both Android and iOS.
     *
     * @param fcmToken  The device's FCM registration token
     * @param title     Notification title (shown in notification tray)
     * @param body      Notification body text
     * @param data      Optional key-value data payload (available in app background/foreground)
     */
    public boolean sendToDevice(String fcmToken, String title, String body, Map<String, String> data) {
        if (!firebaseInitialized) {
            log.info("[FCM-DEV] Would send push to token={} | title='{}' body='{}'",
                    fcmToken.substring(0, Math.min(20, fcmToken.length())) + "...", title, body);
            return true; // Return true in dev mode so app logic isn't blocked
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    // Android-specific config
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .setIcon("ic_alert")
                                    .setColor("#F43F5E")  // TrAI red for critical alerts
                                    .setSound("default")
                                    .build())
                            .build())
                    // iOS (APNs via FCM) config
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setAlert(ApsAlert.builder()
                                            .setTitle(title)
                                            .setBody(body)
                                            .build())
                                    .setSound("default")
                                    .setBadge(1)
                                    .setContentAvailable(true) // Wake app in background
                                    .build())
                            .build())
                    // Cross-platform notification (fallback)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            // Add data payload if provided
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String messageId = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("[FCM] Push sent successfully | messageId={} title='{}'", messageId, title);
            return true;

        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Failed to send push notification: {} ({})",
                    e.getMessage(), e.getMessagingErrorCode());
            return false;
        }
    }

    /**
     * Send push notifications to multiple device tokens in a single batch call.
     * FCM supports up to 500 tokens per batch — we chunk automatically.
     *
     * @param fcmTokens  List of device FCM tokens
     * @param title      Notification title
     * @param body       Notification body
     * @param data       Optional data payload
     */
    public BatchResult sendToMultipleDevices(List<String> fcmTokens, String title, String body,
                                              Map<String, String> data) {
        if (fcmTokens == null || fcmTokens.isEmpty()) {
            return new BatchResult(0, 0, 0);
        }

        if (!firebaseInitialized) {
            log.info("[FCM-DEV] Would send batch push to {} devices | title='{}'",
                    fcmTokens.size(), title);
            return new BatchResult(fcmTokens.size(), fcmTokens.size(), 0);
        }

        int successCount = 0;
        int failureCount = 0;

        // Chunk into batches of 500 (FCM limit)
        List<List<String>> chunks = partition(fcmTokens, 500);
        for (List<String> chunk : chunks) {
            MulticastMessage.Builder msgBuilder = MulticastMessage.builder()
                    .addAllTokens(chunk)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .setIcon("ic_alert")
                                    .setColor("#F43F5E")
                                    .setSound("default")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setAlert(ApsAlert.builder()
                                            .setTitle(title)
                                            .setBody(body)
                                            .build())
                                    .setSound("default")
                                    .setBadge(1)
                                    .setContentAvailable(true)
                                    .build())
                            .build())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            if (data != null && !data.isEmpty()) {
                msgBuilder.putAllData(data);
            }

            try {
                BatchResponse response = FirebaseMessaging.getInstance()
                        .sendEachForMulticast(msgBuilder.build());
                successCount += response.getSuccessCount();
                failureCount += response.getFailureCount();
            } catch (FirebaseMessagingException e) {
                log.error("[FCM] Batch send failed: {}", e.getMessage());
                failureCount += chunk.size();
            }
        }

        log.info("[FCM] Batch complete | sent={} success={} failed={}",
                fcmTokens.size(), successCount, failureCount);
        return new BatchResult(fcmTokens.size(), successCount, failureCount);
    }

    /**
     * Subscribe a device token to an FCM topic.
     * Topics can be used for country-level broadcasting (e.g. "country_Armenia").
     */
    public boolean subscribeToTopic(String fcmToken, String topic) {
        if (!firebaseInitialized) {
            log.info("[FCM-DEV] Would subscribe token to topic={}", topic);
            return true;
        }
        try {
            FirebaseMessaging.getInstance()
                    .subscribeToTopic(List.of(fcmToken), sanitizeTopic(topic));
            log.info("[FCM] Subscribed token to topic={}", topic);
            return true;
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Failed to subscribe to topic {}: {}", topic, e.getMessage());
            return false;
        }
    }

    /**
     * Send a notification to all devices subscribed to a topic.
     * E.g. send to topic "country_Armenia" for all Armenian users.
     */
    public boolean sendToTopic(String topic, String title, String body, Map<String, String> data) {
        if (!firebaseInitialized) {
            log.info("[FCM-DEV] Would send to topic={} | title='{}'", topic, title);
            return true;
        }
        try {
            Message.Builder builder = Message.builder()
                    .setTopic(sanitizeTopic(topic))
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH).build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setAlert(ApsAlert.builder().setTitle(title).setBody(body).build())
                                    .setSound("default").setBadge(1).build())
                            .build());

            if (data != null) builder.putAllData(data);

            String messageId = FirebaseMessaging.getInstance().send(builder.build());
            log.info("[FCM] Topic message sent | topic={} messageId={}", topic, messageId);
            return true;
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Failed to send to topic {}: {}", topic, e.getMessage());
            return false;
        }
    }

    // FCM topic names cannot contain spaces or special chars
    private String sanitizeTopic(String topic) {
        return "country_" + topic.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    /**
     * Result of a batch send operation.
     */
    public record BatchResult(int total, int success, int failure) {}
}
