import 'dart:convert';

import 'package:gherkin/src/expect/expect_mimic.dart';
import 'package:http/http.dart' as http;
import 'package:flutter_gherkin/flutter_gherkin.dart';
import 'package:gherkin/gherkin.dart';

class Testability {
  static final contentTypeUtf8 = {'Content-Type': 'application/json; charset=UTF-8'};
  StepContext<FlutterWorld> context;
  Testability(this.context);

  static Future<void> cleanDbAndResetTestabilitySettings() async {
    final response = await http.post(Uri.parse('http://localhost:9081/api/testability/clean_db_and_reset_testability_settings'), headers: contentTypeUtf8);
    ExpectMimic().expect(response.statusCode, 200);
  }

  Future<void> seedNotebookInBazaar(String notebookName) async {
    http.Response response = await httpPost(
        'http://localhost:9081/api/testability/seed_notes?external_identifier=old_learner',
        [
          {'title': notebookName}
        ]);

    context.expect((jsonDecode(response.body) as List).length, 1);

    http.Response response1 = await httpPost(
        'http://localhost:9081/api/testability/share_to_bazaar',
        {'noteTitle': notebookName}
    );
    context.expect(response1.body, 'OK');
  }

  Future<http.Response> httpPost(String uri, Object object) async {
    final response = await http.post(Uri.parse(uri), headers: contentTypeUtf8, body: jsonEncode(object));
    context.expect(response.statusCode, 200);
    return response;
  }
}

