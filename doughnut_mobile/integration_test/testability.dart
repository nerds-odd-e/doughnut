import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_test/flutter_test.dart';

class Testability {
  final contentTypeUtf8 = {'Content-Type': 'application/json; charset=UTF-8'};
  final testabilityBaseUrl = 'http://localhost:9081/api/testability/';

  Future<void> cleanDbAndResetTestabilitySettings() async {
    await testabilityPost('clean_db_and_reset_testability_settings');
  }

  Future<void> seedNotebookInBazaar(String notebookName) async {
    http.Response response = await testabilityPost(
        'inject_notes',
        bodyObject: {
          'external_identifier': 'old_learner',
          'seedNotes': [
          {'title': notebookName}
        ]});

    expect((jsonDecode(response.body) as List).length, 1);

    http.Response response1 = await testabilityPost('share_to_bazaar',
        bodyObject: {'noteTitle': notebookName});
    expect(response1.body, 'OK');
  }

  Future<http.Response> testabilityPost(String uri,
      {Object? bodyObject}) async {
    final response = await http.post(Uri.parse(testabilityBaseUrl + uri),
        headers: contentTypeUtf8, body: jsonEncode(bodyObject));
    expect(response.statusCode, 200);
    return response;
  }
}
