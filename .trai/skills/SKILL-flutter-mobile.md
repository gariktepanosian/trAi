# SKILL: Flutter Mobile App
# TrAI Platform — iOS + Android (Flutter 3.x)

## What this skill covers

Flutter project structure, screen patterns, state management, service layer conventions,
push notification integration, country picker usage, and rules for adding new screens.

---

## Project Location

```
mobile/
├── pubspec.yaml                  # Dependencies — always pin versions
└── lib/
    ├── main.dart                 # App entry point + Firebase init + Provider setup
    ├── screens/
    │   ├── home_screen.dart      # Main shell: AppBar + NavigationBar + IndexedStack
    │   ├── live_fact_check_screen.dart  # Tab 0 — Speaker + statement → AI verdict
    │   ├── news_screen.dart             # Tab 1 — AI-verified news feed
    │   ├── alerts_screen.dart           # Tab 2 — Critical incident center
    │   └── settings_screen.dart         # Tab 3 — Country picker + AI engine info
    └── services/
        ├── api_service.dart      # All HTTP calls to backend (static methods)
        ├── country_service.dart  # ChangeNotifier — country selection state
        └── notification_service.dart  # FCM init + local notification display
```

---

## Tech Stack

| Package | Version | Purpose |
|---|---|---|
| `firebase_core` | ^3.3.0 | Firebase initialization |
| `firebase_messaging` | ^15.1.0 | FCM push notifications |
| `flutter_local_notifications` | ^17.2.1 | Show notifications in foreground |
| `provider` | ^6.1.1 | State management (CountryService) |
| `shared_preferences` | ^2.2.2 | Local storage (country, FCM token) |
| `country_picker` | ^2.0.26 | Country selection bottom sheet |
| `http` | ^1.2.0 | HTTP client for backend API |
| `intl` | ^0.19.0 | Date/number formatting |

**Dependency rule:** Always pin major + minor version (`^X.Y.Z`). Never use `any` or `>=`.
After adding a package, run `flutter pub get` and verify `pubspec.lock` is committed.

---

## State Management

Only **one** `ChangeNotifier` exists: `CountryService`.
All other state is local (`StatefulWidget` + `setState()`).

### `CountryService` (global state)
```dart
class CountryService extends ChangeNotifier {
    String _selectedCountry = '';   // Persisted in SharedPreferences
    bool _isSubscribed = false;

    // Key methods:
    Future<bool> selectCountry(String country)  // subscribe + persist
    Future<void> clearCountry()                 // unsubscribe + clear
}
```

Provided at app root in `main.dart`:
```dart
MultiProvider(
    providers: [ChangeNotifierProvider(create: (_) => CountryService())],
    child: const TrAiApp(),
)
```

Consumed in screens with `context.watch<CountryService>()` (rebuild on change)
or `context.read<CountryService>()` (one-time read, no rebuild).

---

## Screen Pattern

All screens are `StatefulWidget`. Pattern:

```dart
class MyScreen extends StatefulWidget {
    const MyScreen({super.key});
    @override
    State<MyScreen> createState() => _MyScreenState();
}

class _MyScreenState extends State<MyScreen> {
    bool _isLoading = false;
    List<dynamic> _data = [];

    @override
    void initState() {
        super.initState();
        _loadData();  // Always load in initState
    }

    Future<void> _loadData() async {
        if (_isLoading) return;  // Guard against double-load
        setState(() => _isLoading = true);
        final data = await ApiService.fetchSomething();
        if (mounted) {  // ALWAYS check mounted before setState after await
            setState(() {
                _data = data;
                _isLoading = false;
            });
        }
    }

    @override
    Widget build(BuildContext context) {
        // ...
    }
}
```

**Critical rule:** Always check `if (mounted)` before calling `setState()` after any `await`.
Forgetting this causes "setState() called after dispose()" crashes.

---

## Navigation

`HomeScreen` uses `IndexedStack` (not `Navigator.push`).
This means all 4 tab screens are always alive in the widget tree.
Use `IndexedStack` benefits: screen state is preserved when switching tabs.

```dart
// In home_screen.dart
body: IndexedStack(
    index: _currentIndex,
    children: screens,
),
```

Never use `Navigator.push()` for tab navigation.
Use `Navigator.push()` only for modal detail screens (not yet implemented).

---

## API Service Pattern

All backend calls are static methods on `ApiService`:

```dart
class ApiService {
    static const String baseUrl = 'http://10.0.2.2:8080/api/v1';
    //  ↑ Android emulator. Change to Cloud Run URL for production.

    static Future<Map<String, dynamic>> fetchSomething() async {
        try {
            final response = await http.get(Uri.parse('$baseUrl/endpoint'))
                .timeout(const Duration(seconds: 6));
            if (response.statusCode == 200) return jsonDecode(response.body);
        } catch (_) {}
        return {'fallback': 'data'};  // ALWAYS return fallback, never throw
    }
}
```

**Rules:**
- All methods are `static` — no instance needed
- All methods have a `try/catch` that returns a safe fallback
- All methods have `.timeout()` — never omit this
- Multi-agent calls use `Duration(seconds: 90)` — they take time
- Never call API methods from `build()` — only from `initState()` or user interaction handlers

---

## Colors & Theme

All colors are defined in `main.dart` `ThemeData`. Never hardcode colors in screens.
Use `Theme.of(context).colorScheme.*` or these explicit constants:

| Usage | Value |
|---|---|
| Primary cyan | `Color(0xFF06B6D4)` |
| Secondary blue | `Color(0xFF3B82F6)` |
| Success green | `Color(0xFF10B981)` |
| Warning amber | `Color(0xFFF59E0B)` |
| Error/critical red | `Color(0xFFF43F5E)` |
| Card background | `Color(0xFF131A2B)` |
| Screen background | `Color(0xFF0A0D14)` |

---

## Country Picker Usage

```dart
showCountryPicker(
    context: context,
    showPhoneCode: false,
    countryListTheme: CountryListThemeData(
        backgroundColor: const Color(0xFF111726),
        // ... match TrAI dark theme
    ),
    onSelect: (Country country) async {
        final success = await countryService.selectCountry(country.name);
        // Show SnackBar feedback
    },
);
```

The `country.name` is passed to the backend subscribe endpoint.
The `country_picker` package uses ISO country names — these are also passed
to `TwitterApiService.buildCriticalQuery()` as search terms.

---

## Push Notification Deep Linking

When a user taps a push notification, they should be routed to the Alerts tab.
This is handled by reading `message.data['type']` in the notification handler.

**Not yet implemented** — to implement:
1. In `main.dart` background handler and `HomeScreen`, add:
   ```dart
   FirebaseMessaging.onMessageOpenedApp.listen((message) {
       if (message.data['type'] == 'COUNTRY_ALERT') {
           // Navigate to tab index 2 (Alerts)
       }
   });
   ```
2. Pass `_currentIndex` setter down from `HomeScreen` or use a global nav key

---

## Adding a New Screen

1. Create `mobile/lib/screens/<feature>_screen.dart`
2. Add route in `home_screen.dart` `screens` list (if it's a tab) or use `Navigator.push`
3. If it needs global state, add a new `ChangeNotifier` and register it in `main.dart` `MultiProvider`
4. If it calls the backend, add the method to `api_service.dart`
5. Write Playwright tests in `tests/ui/<feature>.spec.ts` (see `SKILL-playwright-testing.md`)

---

## Playwright Test Coverage Trigger

After any change to a Flutter screen or service:

- `tests/ui/home.spec.ts` — app loads, 4 tabs visible, status indicator shown
- `tests/ui/live-fact-check.spec.ts` — fill speaker + statement, click Audit, verify result card
- `tests/ui/news.spec.ts` — news tab loads, trust score chips visible
- `tests/ui/alerts.spec.ts` — alerts tab loads, filter bar works, resolve button fires API call
- `tests/ui/settings.spec.ts` — country picker opens, selecting country calls subscribe API, manual check button works

Note: Playwright UI tests run against the **frontend web app** (`http://localhost:3000`),
not the Flutter app directly (Flutter has its own widget tests via `flutter test`).
The Flutter `pubspec.yaml` includes `flutter_test` for widget-level tests.
