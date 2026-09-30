import 'package:flutter/material.dart';
import '../services/api_service.dart';

/// AlertsScreen — Real-time Security & Incident Center.
/// Shows platform alerts from the multi-agent validation engine.
/// CRITICAL alerts (tornado, war, earthquake, etc.) appear at the top in red.
class AlertsScreen extends StatefulWidget {
  const AlertsScreen({super.key});

  @override
  State<AlertsScreen> createState() => _AlertsScreenState();
}

class _AlertsScreenState extends State<AlertsScreen> {
  List<dynamic> _alerts = [];
  bool _isLoading = false;
  String _filter = 'ALL'; // ALL | CRITICAL | UNRESOLVED

  @override
  void initState() {
    super.initState();
    _loadAlerts();
  }

  Future<void> _loadAlerts() async {
    if (_isLoading) return;
    setState(() => _isLoading = true);
    final alerts = await ApiService.fetchAlerts();
    if (mounted) {
      setState(() {
        _alerts = alerts;
        _isLoading = false;
      });
    }
  }

  Future<void> _resolveAlert(String alertId, int index) async {
    final success = await ApiService.resolveAlert(alertId);
    if (success && mounted) {
      setState(() {
        _alerts[index]['resolved'] = true;
      });
    }
  }

  List<dynamic> get _filteredAlerts {
    switch (_filter) {
      case 'CRITICAL':
        return _alerts
            .where((a) => a['severity'] == 'CRITICAL')
            .toList();
      case 'UNRESOLVED':
        return _alerts.where((a) => a['resolved'] != true).toList();
      default:
        return _alerts;
    }
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _loadAlerts,
      color: const Color(0xFF06B6D4),
      child: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(child: _buildHeader()),
          SliverToBoxAdapter(child: _buildFilterBar()),
          if (_isLoading)
            const SliverFillRemaining(
              child: Center(
                  child: CircularProgressIndicator(
                      color: Color(0xFF06B6D4))),
            )
          else if (_filteredAlerts.isEmpty)
            SliverFillRemaining(
              child: Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.check_circle_outline,
                        color: Color(0xFF10B981), size: 64),
                    const SizedBox(height: 16),
                    const Text('No active alerts',
                        style: TextStyle(
                            color: Colors.white60, fontSize: 16)),
                    const SizedBox(height: 8),
                    Text(
                      _filter == 'ALL'
                          ? 'The system is monitoring — all clear'
                          : 'No $_filter alerts at this time',
                      style: const TextStyle(color: Colors.white38),
                    ),
                  ],
                ),
              ),
            )
          else
            SliverList(
              delegate: SliverChildBuilderDelegate(
                (ctx, i) {
                  final alert = _filteredAlerts[i];
                  final originalIndex = _alerts.indexOf(alert);
                  return _buildAlertCard(alert, originalIndex);
                },
                childCount: _filteredAlerts.length,
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    final criticalCount =
        _alerts.where((a) => a['severity'] == 'CRITICAL').length;
    final unresolvedCount = _alerts.where((a) => a['resolved'] != true).length;

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
      child: Row(
        children: [
          const Icon(Icons.security, color: Color(0xFF06B6D4), size: 20),
          const SizedBox(width: 8),
          const Text('Incident Center',
              style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Colors.white)),
          const Spacer(),
          if (criticalCount > 0)
            Container(
              padding:
                  const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: const Color(0xFFF43F5E).withOpacity(0.2),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                    color: const Color(0xFFF43F5E).withOpacity(0.5)),
              ),
              child: Text(
                '$criticalCount CRITICAL',
                style: const TextStyle(
                    color: Color(0xFFF43F5E),
                    fontSize: 11,
                    fontWeight: FontWeight.bold),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildFilterBar() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: ['ALL', 'CRITICAL', 'UNRESOLVED'].map((filter) {
            final isSelected = _filter == filter;
            return Padding(
              padding: const EdgeInsets.only(right: 8),
              child: ChoiceChip(
                label: Text(filter),
                selected: isSelected,
                onSelected: (_) => setState(() => _filter = filter),
                selectedColor: const Color(0xFF06B6D4).withOpacity(0.2),
                labelStyle: TextStyle(
                  color: isSelected
                      ? const Color(0xFF06B6D4)
                      : Colors.white54,
                  fontWeight: isSelected
                      ? FontWeight.bold
                      : FontWeight.normal,
                  fontSize: 12,
                ),
              ),
            );
          }).toList(),
        ),
      ),
    );
  }

  Widget _buildAlertCard(dynamic alert, int index) {
    final severity = alert['severity'] ?? 'INFO';
    final bool resolved = alert['resolved'] == true;

    Color severityColor;
    IconData severityIcon;
    switch (severity) {
      case 'CRITICAL':
        severityColor = const Color(0xFFF43F5E);
        severityIcon = Icons.warning_amber_rounded;
        break;
      case 'HIGH':
        severityColor = const Color(0xFFF97316);
        severityIcon = Icons.error_outline;
        break;
      default:
        severityColor = const Color(0xFF3B82F6);
        severityIcon = Icons.info_outline;
    }

    return Opacity(
      opacity: resolved ? 0.5 : 1.0,
      child: Card(
        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: BorderSide(
            color: resolved
                ? Colors.white10
                : severityColor.withOpacity(0.3),
          ),
        ),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(severityIcon, color: severityColor, size: 18),
                  const SizedBox(width: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: severityColor.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(severity,
                        style: TextStyle(
                            color: severityColor,
                            fontSize: 11,
                            fontWeight: FontWeight.bold)),
                  ),
                  const SizedBox(width: 8),
                  Flexible(
                    child: Text(
                      alert['eventType'] ?? '',
                      style: const TextStyle(
                          color: Colors.white54, fontSize: 11),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  const Spacer(),
                  if (resolved)
                    const Icon(Icons.check_circle,
                        color: Color(0xFF10B981), size: 18),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                alert['message'] ?? '',
                style: const TextStyle(
                    color: Colors.white, fontSize: 14, height: 1.4),
              ),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Source: ${alert['source'] ?? 'Unknown'}',
                    style: const TextStyle(
                        color: Colors.white38, fontSize: 11),
                  ),
                  if (!resolved && alert['id'] != null)
                    TextButton.icon(
                      onPressed: () => _resolveAlert(
                          alert['id'].toString(), index),
                      icon: const Icon(Icons.check, size: 14),
                      label: const Text('Resolve',
                          style: TextStyle(fontSize: 12)),
                      style: TextButton.styleFrom(
                        foregroundColor: const Color(0xFF10B981),
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 4),
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
