import 'package:flutter/material.dart';
import 'package:country_picker/country_picker.dart';
import 'package:provider/provider.dart';
import '../services/country_service.dart';
import '../services/notification_service.dart';
import '../services/api_service.dart';

/// SettingsScreen — Enterprise Settings & Monitoring Configuration.
///
/// Key features:
///  - Country selection (triggers push notification subscription)
///  - Push notification status
///  - Manual country check trigger
///  - About / AI model info
class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool _isCheckingCountry = false;
  Map<String, dynamic>? _lastCheckResult;

  Future<void> _triggerManualCheck(String country) async {
    setState(() {
      _isCheckingCountry = true;
      _lastCheckResult = null;
    });

    final result = await ApiService.triggerCountryCheck(country);

    if (mounted) {
      setState(() {
        _isCheckingCountry = false;
        _lastCheckResult = result;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final countryService = context.watch<CountryService>();

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _sectionHeader(Icons.location_on, 'Country Monitoring'),
          _buildCountryCard(countryService),
          const SizedBox(height: 16),

          _sectionHeader(Icons.notifications, 'Push Notifications'),
          _buildNotificationsCard(countryService),
          const SizedBox(height: 16),

          _sectionHeader(Icons.psychology, 'AI Engine'),
          _buildAiEngineCard(),
          const SizedBox(height: 16),

          _sectionHeader(Icons.info_outline, 'About TrAI'),
          _buildAboutCard(),
          const SizedBox(height: 32),
        ],
      ),
    );
  }

  // ─── Country Card ────────────────────────────────────────────────────────

  Widget _buildCountryCard(CountryService countryService) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Monitor your country for critical events',
              style: TextStyle(color: Colors.white70, fontSize: 13),
            ),
            const SizedBox(height: 4),
            const Text(
              'TrAI monitors Twitter/X every 10 minutes. '
              'If Grok, Gemini, and ChatGPT all agree something critical is happening '
              '(tornado, war, earthquake, emergency), you get a push notification.',
              style: TextStyle(color: Colors.white38, fontSize: 12, height: 1.5),
            ),
            const SizedBox(height: 16),

            // Current country display
            if (countryService.hasCountry)
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: const Color(0xFF06B6D4).withOpacity(0.1),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(
                      color: const Color(0xFF06B6D4).withOpacity(0.3)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.location_on,
                        color: Color(0xFF06B6D4), size: 20),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            countryService.selectedCountry,
                            style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                                fontSize: 15),
                          ),
                          const Text(
                            'Monitoring active',
                            style: TextStyle(
                                color: Color(0xFF10B981), fontSize: 12),
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.close,
                          color: Colors.white38, size: 18),
                      onPressed: () => countryService.clearCountry(),
                    ),
                  ],
                ),
              ),

            const SizedBox(height: 12),

            // Select / Change Country button
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: () => _showCountryPicker(context, countryService),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF06B6D4),
                  foregroundColor: Colors.black,
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(10)),
                ),
                icon: const Icon(Icons.search, size: 18),
                label: Text(
                  countryService.hasCountry
                      ? 'Change Country'
                      : 'Select Your Country',
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
              ),
            ),

            // Manual check button
            if (countryService.hasCountry) ...[
              const SizedBox(height: 10),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: _isCheckingCountry
                      ? null
                      : () => _triggerManualCheck(
                          countryService.selectedCountry),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFF06B6D4),
                    side: const BorderSide(color: Color(0xFF06B6D4)),
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(10)),
                  ),
                  icon: _isCheckingCountry
                      ? const SizedBox(
                          width: 14,
                          height: 14,
                          child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Color(0xFF06B6D4)),
                        )
                      : const Icon(Icons.refresh, size: 18),
                  label: Text(
                    _isCheckingCountry
                        ? 'Running AI Analysis...'
                        : 'Check Now (Grok + Gemini + ChatGPT)',
                    style: const TextStyle(fontSize: 13),
                  ),
                ),
              ),
              if (_lastCheckResult != null) _buildCheckResultCard(),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildCheckResultCard() {
    final r = _lastCheckResult!;
    final bool isCritical = r['criticalAlert'] == true;
    final int score = (r['trustScore'] ?? 0) as int;
    final String verdict = r['verdict'] ?? 'UNKNOWN';
    final String level = r['criticalLevel'] ?? 'LOW';

    return Container(
      margin: const EdgeInsets.only(top: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: isCritical
            ? const Color(0xFFF43F5E).withOpacity(0.1)
            : const Color(0xFF10B981).withOpacity(0.1),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(
          color: isCritical
              ? const Color(0xFFF43F5E).withOpacity(0.4)
              : const Color(0xFF10B981).withOpacity(0.4),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                isCritical ? Icons.warning_amber : Icons.check_circle,
                color: isCritical
                    ? const Color(0xFFF43F5E)
                    : const Color(0xFF10B981),
                size: 18,
              ),
              const SizedBox(width: 8),
              Text(
                isCritical ? 'CRITICAL ALERT DETECTED' : 'Situation Normal',
                style: TextStyle(
                  color: isCritical
                      ? const Color(0xFFF43F5E)
                      : const Color(0xFF10B981),
                  fontWeight: FontWeight.bold,
                  fontSize: 13,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text('Verdict: $verdict',
              style: const TextStyle(color: Colors.white70, fontSize: 13)),
          Text('Trust Score: $score%',
              style: const TextStyle(color: Colors.white70, fontSize: 13)),
          Text('Level: $level',
              style: const TextStyle(color: Colors.white70, fontSize: 13)),
          Text('Tweets analyzed: ${r['tweetsAnalyzed'] ?? 0}',
              style: const TextStyle(color: Colors.white38, fontSize: 12)),
        ],
      ),
    );
  }

  void _showCountryPicker(BuildContext context, CountryService countryService) {
    showCountryPicker(
      context: context,
      showPhoneCode: false,
      countryListTheme: CountryListThemeData(
        backgroundColor: const Color(0xFF111726),
        textStyle: const TextStyle(color: Colors.white),
        searchTextStyle: const TextStyle(color: Colors.white),
        inputDecoration: InputDecoration(
          hintText: 'Search country...',
          hintStyle: const TextStyle(color: Colors.white38),
          prefixIcon: const Icon(Icons.search, color: Colors.white38),
          filled: true,
          fillColor: const Color(0xFF1A2236),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: BorderSide.none,
          ),
        ),
        borderRadius: const BorderRadius.only(
          topLeft: Radius.circular(16),
          topRight: Radius.circular(16),
        ),
      ),
      onSelect: (Country country) async {
        final success = await countryService.selectCountry(country.name);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(success
                  ? 'Monitoring ${country.name} — you\'ll get push alerts for critical events'
                  : 'Subscribed to ${country.name} (demo mode)'),
              backgroundColor:
                  success ? const Color(0xFF10B981) : const Color(0xFFF59E0B),
            ),
          );
        }
      },
    );
  }

  // ─── Notifications Card ──────────────────────────────────────────────────

  Widget _buildNotificationsCard(CountryService countryService) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.notifications_active,
                    color: Color(0xFF06B6D4), size: 20),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Critical Event Alerts',
                          style: TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold)),
                      Text(
                        countryService.hasCountry
                            ? 'Enabled for ${countryService.selectedCountry}'
                            : 'Select a country to enable',
                        style: const TextStyle(
                            color: Colors.white54, fontSize: 12),
                      ),
                    ],
                  ),
                ),
                Icon(
                  countryService.hasCountry
                      ? Icons.check_circle
                      : Icons.radio_button_unchecked,
                  color: countryService.hasCountry
                      ? const Color(0xFF10B981)
                      : Colors.white38,
                ),
              ],
            ),
            const Divider(height: 24, color: Colors.white10),
            const Text(
              'You receive a push notification when:',
              style: TextStyle(color: Colors.white54, fontSize: 12),
            ),
            const SizedBox(height: 8),
            ...[
              'Grok, Gemini, AND ChatGPT all confirm the event',
              'Trust score exceeds 55% consensus threshold',
              'Event type: tornado, war, earthquake, attack, emergency',
              'Real Twitter/X users + verified accounts reporting',
            ].map((item) => Padding(
                  padding: const EdgeInsets.only(bottom: 4),
                  child: Row(
                    children: [
                      const Icon(Icons.chevron_right,
                          color: Color(0xFF06B6D4), size: 14),
                      const SizedBox(width: 4),
                      Flexible(
                        child: Text(item,
                            style: const TextStyle(
                                color: Colors.white60, fontSize: 12)),
                      ),
                    ],
                  ),
                )),
          ],
        ),
      ),
    );
  }

  // ─── AI Engine Card ───────────────────────────────────────────────────────

  Widget _buildAiEngineCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            _aiModelRow(
              'Grok-2-1212',
              'xAI (Twitter/X native)',
              const Color(0xFF06B6D4),
              'Real tweet context, X user sentiment, Grok trust scoring',
            ),
            const Divider(height: 20, color: Colors.white10),
            _aiModelRow(
              'Gemini 2.0 Flash',
              'Google Vertex AI',
              const Color(0xFF4285F4),
              'Google knowledge graph, geographic validation, news signals',
            ),
            const Divider(height: 20, color: Colors.white10),
            _aiModelRow(
              'GPT-4o',
              'OpenAI ChatGPT',
              const Color(0xFF10B981),
              'Safety filter, propaganda detection, push notify decision',
            ),
            const Divider(height: 20, color: Colors.white10),
            const Row(
              children: [
                Icon(Icons.account_tree, color: Colors.purple, size: 18),
                SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Dual-Agent Pipeline',
                          style: TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                              fontSize: 13)),
                      Text(
                        'Agent 1 (Collector) + Agent 2 (Validator) run in parallel, '
                        'each calling all 3 AIs simultaneously',
                        style:
                            TextStyle(color: Colors.white54, fontSize: 11, height: 1.4),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _aiModelRow(
      String model, String provider, Color color, String description) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 8,
          height: 8,
          margin: const EdgeInsets.only(top: 4),
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(model,
                      style: TextStyle(
                          color: color,
                          fontWeight: FontWeight.bold,
                          fontSize: 13)),
                  const SizedBox(width: 6),
                  Text('($provider)',
                      style: const TextStyle(
                          color: Colors.white38, fontSize: 11)),
                ],
              ),
              Text(description,
                  style: const TextStyle(
                      color: Colors.white54,
                      fontSize: 11,
                      height: 1.4)),
            ],
          ),
        ),
      ],
    );
  }

  // ─── About Card ───────────────────────────────────────────────────────────

  Widget _buildAboutCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Text('TrAI Platform',
                    style: TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 15)),
                SizedBox(width: 8),
                Chip(
                  label: Text('v2.0.0',
                      style: TextStyle(fontSize: 11)),
                  padding: EdgeInsets.zero,
                  materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                ),
              ],
            ),
            const SizedBox(height: 8),
            const Text(
              'Real-time truth infrastructure powered by multi-agent AI consensus.\n'
              'Deployed on Google Cloud (Cloud Run, Vertex AI, GCS, Firebase).',
              style: TextStyle(
                  color: Colors.white54, fontSize: 12, height: 1.5),
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                'Spring Boot 4',
                'Google Cloud Run',
                'Vertex AI',
                'Firebase FCM',
                'Twitter API v2',
                'LangChain4j',
              ]
                  .map((tech) => Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.white10,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(tech,
                            style: const TextStyle(
                                color: Colors.white60, fontSize: 11)),
                      ))
                  .toList(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _sectionHeader(IconData icon, String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8, top: 4),
      child: Row(
        children: [
          Icon(icon, color: const Color(0xFF06B6D4), size: 16),
          const SizedBox(width: 8),
          Text(title,
              style: const TextStyle(
                  color: Colors.white70,
                  fontSize: 13,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 0.5)),
        ],
      ),
    );
  }
}
