import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'api_service.dart';
import 'notification_service.dart';

/// CountryService — manages the user's selected country and
/// registers/unregisters FCM push notification subscriptions.
class CountryService extends ChangeNotifier {
  String _selectedCountry = '';
  bool _isSubscribed = false;

  String get selectedCountry => _selectedCountry;
  bool get isSubscribed => _isSubscribed;
  bool get hasCountry => _selectedCountry.isNotEmpty;

  CountryService() {
    _loadSavedCountry();
  }

  Future<void> _loadSavedCountry() async {
    final prefs = await SharedPreferences.getInstance();
    final saved = prefs.getString('selected_country') ?? '';
    if (saved.isNotEmpty) {
      _selectedCountry = saved;
      _isSubscribed = true;
      notifyListeners();
    }
  }

  /// Called when user selects a new country.
  /// Unsubscribes from old country, subscribes to new one.
  Future<bool> selectCountry(String country) async {
    final prefs = await SharedPreferences.getInstance();
    final token = await NotificationService.getFcmToken();

    // Unsubscribe from old country
    if (_selectedCountry.isNotEmpty && token != null) {
      await ApiService.unsubscribeFromCountry(
        country: _selectedCountry,
        fcmToken: token,
      );
    }

    // Subscribe to new country
    bool success = true;
    if (token != null && token.isNotEmpty) {
      final result = await ApiService.subscribeToCountry(
        country: country,
        fcmToken: token,
      );
      success = result['status'] == 'SUBSCRIBED';
    }

    // Save locally
    _selectedCountry = country;
    _isSubscribed = success;
    await prefs.setString('selected_country', country);
    notifyListeners();

    return success;
  }

  Future<void> clearCountry() async {
    final prefs = await SharedPreferences.getInstance();
    final token = await NotificationService.getFcmToken();
    if (_selectedCountry.isNotEmpty && token != null) {
      await ApiService.unsubscribeFromCountry(
        country: _selectedCountry,
        fcmToken: token,
      );
    }
    _selectedCountry = '';
    _isSubscribed = false;
    await prefs.remove('selected_country');
    notifyListeners();
  }
}
