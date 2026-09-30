import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:provider/provider.dart';
import 'screens/home_screen.dart';
import 'services/notification_service.dart';
import 'services/country_service.dart';

// Background message handler — must be top-level function
@pragma('vm:entry-point')
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
  await NotificationService.showLocalNotification(message);
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize Firebase (requires google-services.json on Android,
  // GoogleService-Info.plist on iOS — both from Firebase Console)
  try {
    await Firebase.initializeApp();
    FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);
  } catch (e) {
    // Firebase not configured yet — app runs in demo mode
    debugPrint('[Firebase] Not initialized: $e');
  }

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => CountryService()),
      ],
      child: const TrAiApp(),
    ),
  );
}

class TrAiApp extends StatelessWidget {
  const TrAiApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'TrAI Platform',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0A0D14),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF06B6D4),
          secondary: Color(0xFF3B82F6),
          surface: Color(0xFF111726),
          error: Color(0xFFF43F5E),
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF111726),
          elevation: 0,
          centerTitle: false,
        ),
        cardTheme: CardTheme(
          color: const Color(0xFF131A2B),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: const BorderSide(color: Colors.white10),
          ),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: const Color(0xFF1A2236),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: const BorderSide(color: Colors.white24),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: const BorderSide(color: Colors.white24),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: const BorderSide(color: Color(0xFF06B6D4)),
          ),
          labelStyle: const TextStyle(color: Colors.white54),
        ),
        chipTheme: ChipThemeData(
          backgroundColor: const Color(0xFF1A2236),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
        fontFamily: 'Roboto',
      ),
      home: const HomeScreen(),
    );
  }
}
