import 'package:flutter/material.dart';
import '../services/api_service.dart';

class LiveFactCheckScreen extends StatefulWidget {
  const LiveFactCheckScreen({super.key});

  @override
  State<LiveFactCheckScreen> createState() => _LiveFactCheckScreenState();
}

class _LiveFactCheckScreenState extends State<LiveFactCheckScreen> {
  final _speakerController = TextEditingController(text: 'Press Secretary');
  final _statementController = TextEditingController(
    text: 'Unemployment dropped by two percent in the last quarter across all major sectors.',
  );
  bool _isLoading = false;
  Map<String, dynamic>? _result;

  Future<void> _auditStatement() async {
    setState(() => _isLoading = true);
    final res = await ApiService.verifyLiveStatement(
      speaker: _speakerController.text.trim(),
      statement: _statementController.text.trim(),
    );
    setState(() {
      _isLoading = false;
      _result = res;
    });
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.mic, color: Color(0xFF06B6D4)),
                      const SizedBox(width: 8),
                      Text(
                        'Live Speech & Audio Stream',
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                            ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _speakerController,
                    decoration: const InputDecoration(
                      labelText: 'Speaker Identity',
                      border: OutlineInputBorder(),
                      isDense: true,
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _statementController,
                    maxLines: 3,
                    decoration: const InputDecoration(
                      labelText: 'Spoken Statement Chunk',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton.icon(
                    onPressed: _isLoading ? null : _auditStatement,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF06B6D4),
                      foregroundColor: Colors.black,
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    icon: _isLoading
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.black),
                          )
                        : const Icon(Icons.bolt),
                    label: Text(_isLoading ? 'Auditing Consensus...' : 'Audit Live Statement'),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          if (_result != null) _buildResultCard(),
        ],
      ),
    );
  }

  Widget _buildResultCard() {
    final score = _result?['trustScore'] ?? 85;
    final verdict = _result?['verdict'] ?? 'VERIFIED';
    final analysis = _result?['analysis'] ?? 'Claim analyzed.';

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Chip(
                  backgroundColor: score > 70 ? Colors.green.shade900 : Colors.amber.shade900,
                  label: Text(
                    verdict.toString(),
                    style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
                  ),
                ),
                Text(
                  'Trust Score: $score%',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: score > 70 ? const Color(0xFF10B981) : const Color(0xFFF59E0B),
                  ),
                ),
              ],
            ),
            const Divider(height: 24),
            const Text(
              'Empirical Consensus Breakdown',
              style: TextStyle(fontSize: 12, color: Colors.grey, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              analysis.toString(),
              style: const TextStyle(fontSize: 14, height: 1.5, color: Colors.white70),
            ),
          ],
        ),
      ),
    );
  }
}
