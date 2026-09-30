import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'api_service.dart';

/// NotificationService — handles FCM push notifications on Android and iOS.
///
/// Setup:
///  Android: google-services.json in android/app/
///  iOS:     GoogleService-Info.plist in ios/Runner/ + APNs key in Firebase Console
class NotificationService {
  static final FlutterLocalNotificationsPlugin _localNotifications =
      FlutterLocalNotificationsPlugin();
  static FirebaseMessaging? _messaging;

  static const AndroidNotificationChannel _criticalChannel =
      AndroidNotificationChannel(
    'trai_critical_alerts',
    'TrAI Critical Alerts',
    description: 'High-priority alerts about critical events in your country',
    importance: Importance.max,
    playSound: true,
    enableVibration: true,
  );

  /// Initialize FCM and local notifications.
  /// Call this from HomeScreen after Firebase.initializeApp().
  static Future<String?> initialize() async {
    // Initialize local notifications
    const androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosInit = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );
    await _localNotifications.initialize(
      const InitializationSettings(android: androidInit, iOS: iosInit),
    );

    // Create Android notification channel
    await _localNotifications
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(_criticalChannel);

    // Initialize FCM
    try {
      _messaging = FirebaseMessaging.instance;

      // Request permission (iOS requires explicit request)
      final settings = await _messaging!.requestPermission(
        alert: true,
        announcement: false,
        badge: true,
        carPlay: false,
        criticalAlert: true, // iOS critical alerts (bypass Do Not Disturb)
        provisional: false,
        sound: true,
      );

      if (settings.authorizationStatus == AuthorizationStatus.authorized ||
          settings.authorizationStatus == AuthorizationStatus.provisional) {
        // Get FCM token
        final token = await _messaging!.getToken();

        // Save token locally
        if (token != null) {
          final prefs = await SharedPreferences.getInstance();
          await prefs.setString('fcm_token', token);
        }

        // Listen for foreground messages
        FirebaseMessaging.onMessage.listen((RemoteMessage message) {
          showLocalNotification(message);
        });

        // Handle token refresh
        _messaging!.onTokenRefresh.listen((newToken) async {
          final prefs = await SharedPreferences.getInstance();
          final country = prefs.getString('selected_country') ?? '';
          if (country.isNotEmpty) {
            await ApiService.subscribeToCountry(country: country, fcmToken: newToken);
          }
          await prefs.setString('fcm_token', newToken);
        });

        return token;
      }
    } catch (e) {
      // Firebase not configured — demo mode
    }
    return null;
  }

  /// Show a local notification for a received FCM message.
  /// Also used as background handler (top-level function in main.dart).
  static Future<void> showLocalNotification(RemoteMessage message) async {
    final notification = message.notification;
    if (notification == null) return;

    const androidDetails = AndroidNotificationDetails(
      'trai_critical_alerts',
      'TrAI Critical Alerts',
      channelDescription: 'High-priority alerts about critical events in your country',
      importance: Importance.max,
      priority: Priority.high,
      color: Color(0xFFF43F5E),
      playSound: true,
      enableVibration: true,
      fullScreenIntent: true,
    );
    const iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
      interruptionLevel: InterruptionLevel.critical,
    );
    const details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    await _localNotifications.show(
      notification.hashCode,
      notification.title,
      notification.body,
      details,
    );
  }

  /// Get the current FCM token (device push token).
  static Future<String?> getFcmToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('fcm_token');
  }
}

// Needed for Color reference in AndroidNotificationDetails
import 'dart:ui' show Color;
