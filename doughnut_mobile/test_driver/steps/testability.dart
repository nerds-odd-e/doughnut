import 'dart:convert';

import 'package:gherkin/src/expect/expect_mimic.dart';
import 'package:http/http.dart' as http;
import 'package:flutter_gherkin/flutter_gherkin.dart';
import 'package:gherkin/gherkin.dart';

class Testability extends TestabilityBase {
  StepContext<FlutterWorld> context;

  Testability(this.context);

  @override
  void expect(
    actual,
    matcher, {
    String? reason,
  }) {
    context.expect(actual, matcher, reason: reason);
  }
}

class TestabilityContextless extends TestabilityBase {
  @override
  void expect(
    actual,
    matcher, {
    String? reason,
  }) {
    ExpectMimic().expect(actual, matcher, reason: reason);
  }
}

abstract class TestabilityBase {
  final contentTypeUtf8 = {'Content-Type': 'application/json; charset=UTF-8'};
  final testabilityBaseUrl = 'http://localhost:9081/api/testability/';

  Future<void> cleanDbAndResetTestabilitySettings() async {
    await testabilityPost('clean_db_and_reset_testability_settings');
  }

  Future<void> seedNotebookInBazaar(String notebookName) async {
    http.Response response = await testabilityPost(
        'seed_notes',
        bodyObject: {
          external_identifier: 'old_learner',
          seedNotes: [
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

  void expect(
    actual,
    matcher, {
    String? reason,
  });
}
