import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:flutter_gherkin/flutter_gherkin.dart';
import 'package:gherkin/gherkin.dart';


class Testability {
  StepContext<FlutterWorld> context;
  Testability(this.context);

  // 'http://localhost:9081/api/testability/clean_db_and_reset_testability_settings',
  seedNotebookInBazaar(String notebookName) async {
    await httpPost(
      'http://localhost:9081/api/testability/clean_db_and_reset_testability_settings',
        [

        ]);

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

  Future<http.Response> httpPost(
      String uri, Object object) async {
    final response = await http.post(Uri.parse(uri),
        headers: {
          'Content-Type': 'application/json; charset=UTF-8',
        },
        body: jsonEncode(object));
    context.expect(response.statusCode, 200);
    return response;
  }
}

