import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'live_fact_check_screen.dart';
import 'news_screen.dart';
import 'alerts_screen.dart';
import 'settings_screen.dart';
import '../services/api_service.dart';
import '../services/notification_service.dart';
import '../services/country_service.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;
  String _backendStatus = 'Checking...';
  bool _notificationsInitialized = false;

  @override
  void initState() {
    super.initState();
    _checkStatus();
    _initNotifications();
  }

  Future<void> _checkStatus() async {
    final status = await ApiService.checkStatus();
    if (mounted) {
      setState(() {
        _backendStatus = status['status'] ?? 'OPERATIONAL';
      });
    }
  }

  Future<void> _initNotifications() async {
    if (_notificationsInitialized) return;
    final token = await NotificationService.initialize();
    _notificationsInitialized = true;

    // If user already has a saved country, re-register token
    if (token != null && mounted) {
      final countryService = context.read<CountryService>();
      if (countryService.hasCountry) {
        await ApiService.subscribeToCountry(
          country: countryService.selectedCountry,
          fcmToken: token,
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final countryService = context.watch<CountryService>();

    final List<Widget> screens = [
      const LiveFactCheckScreen(),
      NewsScreen(country: countryService.selectedCountry),
      const AlertsScreen(),
      const SettingsScreen(),
    ];

    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: const Color(0xFF06B6D4),
                borderRadius: BorderRadius.circular(6),
              ),
              child: const Text('TrAI',
                  style: TextStyle(
                      fontWeight: FontWeight.bold, color: Colors.black)),
            ),
            const SizedBox(width: 10),
            const Text('Truth Infrastructure',
                style: TextStyle(fontSize: 16)),
          ],
        ),
        actions: [
          // Country indicator
          if (countryService.hasCountry)
            Container(
              margin: const EdgeInsets.only(right: 8),
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: Colors.white10,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                children: [
                  const Icon(Icons.location_on,
                      color: Color(0xFF06B6D4), size: 14),
                  const SizedBox(width: 4),
                  Text(
                    countryService.selectedCountry,
                    style: const TextStyle(fontSize: 12),
                  ),
                ],
              ),
            ),
          // Backend status indicator
          Container(
            margin: const EdgeInsets.only(right: 16),
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: Colors.white10,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Row(
              children: [
                Icon(
                  Icons.circle,
                  color: _backendStatus.contains('DEMO')
                      ? Colors.orange
                      : const Color(0xFF10B981),
                  size: 10,
                ),
                const SizedBox(width: 6),
                Text(_backendStatus, style: const TextStyle(fontSize: 12)),
              ],
            ),
          ),
        ],
      ),
      body: IndexedStack(
        index: _currentIndex,
        children: screens,
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (idx) => setState(() => _currentIndex = idx),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.mic_outlined),
            selectedIcon: Icon(Icons.mic),
            label: 'Live Check',
          ),
          NavigationDestination(
            icon: Icon(Icons.article_outlined),
            selectedIcon: Icon(Icons.article),
            label: 'News',
          ),
          NavigationDestination(
            icon: Icon(Icons.notifications_outlined),
            selectedIcon: Icon(Icons.notifications),
            label: 'Alerts',
          ),
          NavigationDestination(
            icon: Icon(Icons.settings_outlined),
            selectedIcon: Icon(Icons.settings),
            label: 'Settings',
          ),
        ],
      ),
    );
  }
}
