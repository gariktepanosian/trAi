# SKILL: Push Notifications
# TrAI Platform — Firebase Cloud Messaging (FCM) for Android + iOS

## What this skill covers

Push notification architecture, Firebase Admin SDK setup, Android and iOS-specific
configuration, sending patterns, and rules for triggering notifications correctly.

---

## File Location

```
backend/src/main/java/com/trai/engine/notifications/
└── PushNotificationService.java
```

---

## Architecture Overview

```
Backend (Spring Boot)
    └── PushNotificationService (Firebase Admin SDK)
            │
            ├── sendToDevice(token, title, body, data)
            │       └── FirebaseMessaging.getInstance().send(Message)
            │
            ├── sendToMultipleDevices(tokens, title, body, data)
            │       └── FirebaseMessaging.getInstance().sendEachForMulticast(MulticastMessage)
            │           Chunked: 500 tokens per batch (FCM limit)
            │
            └── sendToTopic(topic, title, body, data)
                    └── FirebaseMessaging.getInstance().send(topicMessage)

Mobile (Flutter)
    └── NotificationService (firebase_messaging + flutter_local_notifications)
            ├── Foreground:   FirebaseMessaging.onMessage.listen() → showLocalNotification()
            ├── Background:   _firebaseMessagingBackgroundHandler() (top-level function)
            └── Terminated:   FCM delivers directly to system tray
```

---

## Firebase Setup (Required to Go Live)

### Step 1 — Create Firebase Project
1. Go to https://console.firebase.google.com
2. Create project "trai-platform"
3. Enable Firebase Cloud Messaging

### Step 2 — Android Setup
1. Add Android app in Firebase Console (package name: `com.trai.mobile`)
2. Download `google-services.json`
3. Place at: `mobile/android/app/google-services.json`
4. `android/build.gradle` — add Google services classpath
5. `android/app/build.gradle` — apply `com.google.gms.google-services` plugin

### Step 3 — iOS Setup
1. Add iOS app in Firebase Console (bundle ID from your Apple Developer account)
2. Download `GoogleService-Info.plist`
3. Place at: `mobile/ios/Runner/GoogleService-Info.plist`
4. In Firebase Console → Project Settings → Cloud Messaging:
   - Upload APNs Authentication Key (.p8 from Apple Developer → Certificates, Identifiers & Profiles)
   - Or upload APNs Certificate (.p12)
5. FCM handles APNs relay automatically — no extra iOS code needed

### Step 4 — Backend Service Account
1. Firebase Console → Project Settings → Service Accounts
2. Click "Generate new private key"
3. Download JSON file
4. Set env var: `FIREBASE_CREDENTIALS_PATH=/path/to/firebase-service-account.json`

---

## `PushNotificationService` Methods

### `sendToDevice(fcmToken, title, body, data)`
Sends to a single device. Returns `true` on success, `false` on failure.

### `sendToMultipleDevices(tokens, title, body, data)` → `BatchResult`
Sends to a list of device tokens. Auto-chunks into 500-token batches.
Returns `BatchResult(total, success, failure)`.

### `subscribeToTopic(fcmToken, topic)` / `sendToTopic(topic, title, body, data)`
Topic-based broadcasting. Topic names are sanitized to `country_<name>`.
Useful alternative to managing token lists — device subscribes once, server broadcasts to topic.

---

## Android Config (set in `PushNotificationService`)
```java
AndroidConfig.builder()
    .setPriority(AndroidConfig.Priority.HIGH)    // Wake device immediately
    .setNotification(AndroidNotification.builder()
        .setTitle(title)
        .setBody(body)
        .setIcon("ic_alert")                     // Must exist in Android drawable
        .setColor("#F43F5E")                     // TrAI red
        .setSound("default")
        .build())
    .build()
```

## iOS Config (APNs via FCM bridge)
```java
ApnsConfig.builder()
    .setAps(Aps.builder()
        .setAlert(ApsAlert.builder().setTitle(title).setBody(body).build())
        .setSound("default")
        .setBadge(1)
        .setContentAvailable(true)               // Wake app in background
        .build())
    .build()
```

`contentAvailable: true` — lets the Flutter app process the notification in background
even when the user has not tapped it.

---

## Flutter Side

### `notification_service.dart` — Key methods

```dart
// Initialize at app startup (called from HomeScreen.initState)
static Future<String?> initialize()
    → Requests permission (iOS mandatory)
    → Gets FCM token
    → Saves token to SharedPreferences as 'fcm_token'
    → Sets up foreground message handler
    → Sets up token refresh handler (re-subscribes to country on token change)
    → Returns the FCM token String

// Show a local notification (called for foreground + background messages)
static Future<void> showLocalNotification(RemoteMessage message)

// Get saved token
static Future<String?> getFcmToken()
```

### iOS Interrupt Level
```dart
DarwinNotificationDetails(
    interruptionLevel: InterruptionLevel.critical  // Bypasses Do Not Disturb on iOS
)
```

Critical interrupt level requires the `criticalAlert` entitlement from Apple (for emergency apps).
If not approved, use `InterruptionLevel.timeSensitive` instead.

---

## Data Payload

Every push notification includes a `data` map with:
```dart
{
    "country":      "Armenia",
    "verdict":      "LIKELY_TRUE",
    "trustScore":   "72",
    "criticalLevel":"HIGH",
    "alertId":      "mongodb-alert-id",
    "type":         "COUNTRY_ALERT"
}
```

The Flutter app can use `message.data['type']` to route the user to the correct screen
when they tap the notification (e.g., navigate to Alerts tab when `type == "COUNTRY_ALERT"`).

---

## Dev Mode Behavior

When `FIREBASE_CREDENTIALS_PATH` is not set:
- `firebaseInitialized = false`
- All send methods log the notification content at `INFO` level
- All send methods return `true` (success) so app logic is not blocked
- This allows full local development without a Firebase account

---

## Notification Trigger Rules

Push notifications are sent **only** when ALL of these are true:
1. `MultiAgentResult.criticalAlert == true`
2. `MultiAgentResult.consensusTrustScore >= 55`
3. The country has at least one subscribed FCM token
4. `PushNotificationService.firebaseInitialized == true` (or dev mode)

**Never** send a push notification based on a single AI model's output.
The multi-agent consensus (minimum 2 agents × 3 models = 6 votes) is required.

---

## Adding a New Notification Type

1. Define a new `type` value constant (e.g., `"LIVE_FACTCHECK_ALERT"`)
2. Add send call in the appropriate service (e.g., `TrustVerificationService`)
3. Add `type` to the `data` map
4. In Flutter `main.dart` background handler + `HomeScreen`, handle the new type
5. Update `SKILL-push-notifications.md` (this file) with the new type
6. Add Playwright test in `tests/api/notifications.spec.ts` verifying the endpoint
   that triggers the notification returns the expected response

---

## Playwright Test Coverage Trigger

After any change to `PushNotificationService` or notification trigger logic:

- `tests/api/notifications.spec.ts`
  - Test that `POST /api/v1/country/trigger-check` with a critical-keyword tweet mock
    results in an alert being created (check `GET /api/v1/alerts` response)
  - Test that a non-critical result does NOT create an alert
  - FCM actual delivery is NOT testable with Playwright (it requires a real device)
    — use mock assertions on the alert creation side instead
