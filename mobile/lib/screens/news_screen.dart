import 'package:flutter/material.dart';
import '../services/api_service.dart';

/// NewsScreen — shows AI-verified news feed filtered by the user's country.
/// Each news item shows the multi-agent trust score and propaganda score.
class NewsScreen extends StatefulWidget {
  final String country;
  const NewsScreen({super.key, required this.country});

  @override
  State<NewsScreen> createState() => _NewsScreenState();
}

class _NewsScreenState extends State<NewsScreen> {
  List<dynamic> _news = [];
  bool _isLoading = false;
  String? _error;
  String? _lastLoadedCountry;

  @override
  void initState() {
    super.initState();
    _loadNews();
  }

  @override
  void didUpdateWidget(NewsScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.country != widget.country) {
      _loadNews();
    }
  }

  Future<void> _loadNews() async {
    if (_isLoading) return;
    setState(() {
      _isLoading = true;
      _error = null;
    });

    final news = await ApiService.fetchNews(
      country: widget.country.isNotEmpty ? widget.country : null,
    );

    if (mounted) {
      setState(() {
        _news = news;
        _isLoading = false;
        _lastLoadedCountry = widget.country;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _loadNews,
      color: const Color(0xFF06B6D4),
      child: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(
            child: _buildHeader(),
          ),
          if (_isLoading)
            const SliverFillRemaining(
              child: Center(
                child: CircularProgressIndicator(color: Color(0xFF06B6D4)),
              ),
            )
          else if (_news.isEmpty)
            SliverFillRemaining(
              child: Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.newspaper, color: Colors.white30, size: 64),
                    const SizedBox(height: 16),
                    const Text('No news loaded',
                        style: TextStyle(color: Colors.white54)),
                    const SizedBox(height: 8),
                    TextButton(
                        onPressed: _loadNews,
                        child: const Text('Reload')),
                  ],
                ),
              ),
            )
          else
            SliverList(
              delegate: SliverChildBuilderDelegate(
                (ctx, i) => _buildNewsCard(_news[i]),
                childCount: _news.length,
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return Container(
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          const Icon(Icons.article, color: Color(0xFF06B6D4), size: 20),
          const SizedBox(width: 8),
          Text(
            widget.country.isNotEmpty
                ? 'News: ${widget.country}'
                : 'Global News Feed',
            style: const TextStyle(
                fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white),
          ),
          const Spacer(),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: const Color(0xFF0F3460),
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Row(
              children: [
                Icon(Icons.verified, color: Color(0xFF06B6D4), size: 12),
                SizedBox(width: 4),
                Text('AI Verified',
                    style: TextStyle(fontSize: 11, color: Color(0xFF06B6D4))),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNewsCard(dynamic item) {
    final int trustScore = (item['trustScore'] ?? 70) as int;
    final int propagandaScore = (item['propagandaScore'] ?? 20) as int;
    final String title = item['title'] ?? 'Untitled';
    final String summary = item['summary'] ?? '';
    final String sourceName = item['sourceName'] ?? 'Unknown Source';

    Color trustColor;
    if (trustScore >= 80) {
      trustColor = const Color(0xFF10B981);
    } else if (trustScore >= 60) {
      trustColor = const Color(0xFFF59E0B);
    } else {
      trustColor = const Color(0xFFF43F5E);
    }

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Source + trust badge
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(sourceName,
                    style: TextStyle(
                        color: Colors.white54,
                        fontSize: 12,
                        fontWeight: FontWeight.w500)),
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: trustColor.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: trustColor.withOpacity(0.4)),
                      ),
                      child: Text(
                        'Trust $trustScore%',
                        style: TextStyle(
                            color: trustColor,
                            fontSize: 11,
                            fontWeight: FontWeight.bold),
                      ),
                    ),
                    const SizedBox(width: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: Colors.orange.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(
                        'Bias $propagandaScore%',
                        style: const TextStyle(
                            color: Colors.orange,
                            fontSize: 11,
                            fontWeight: FontWeight.bold),
                      ),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text(title,
                style: const TextStyle(
                    color: Colors.white,
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    height: 1.3)),
            if (summary.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(summary,
                  style: const TextStyle(
                      color: Colors.white60, fontSize: 13, height: 1.5)),
            ],
            const SizedBox(height: 10),
            // AI models badge
            Row(
              children: [
                _modelBadge('Grok', const Color(0xFF06B6D4)),
                const SizedBox(width: 6),
                _modelBadge('Gemini', const Color(0xFF4285F4)),
                const SizedBox(width: 6),
                _modelBadge('GPT-4o', const Color(0xFF10B981)),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _modelBadge(String name, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withOpacity(0.12),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(name,
          style: TextStyle(
              color: color, fontSize: 10, fontWeight: FontWeight.bold)),
    );
  }
}
