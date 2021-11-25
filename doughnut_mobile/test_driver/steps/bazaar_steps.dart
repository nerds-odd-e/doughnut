import 'dart:convert';

import 'package:flutter_driver/flutter_driver.dart';
import 'package:flutter_gherkin/flutter_gherkin.dart';
import 'package:gherkin/gherkin.dart';
import 'package:http/http.dart' as http;

List<StepDefinitionGeneric> bazaarSteps() {
  return [
    when1<String, FlutterWorld>(
      'there is a notebook {string} in the bazaar',
      (notebookName, context) async {
        http.Response response = await httpPost(
            'http://localhost:9081/api/testability/seed_notes?external_identifier=old_learner',
            [
              {'title': notebookName}
            ]);

        if (response.statusCode == 200) {
          context.expect((jsonDecode(response.body) as List).length, 1);
        }

        http.Response response1 = await httpPost(
            'http://localhost:9081/api/testability/share_to_bazaar',
            {'noteTitle': notebookName}
            );
        context.expect(response1.body, 'OK');

      },
    ),
    given<FlutterWorld>(
      "I haven't login",
      (context) async {},
    ),
    then1<String, FlutterWorld>(
      "I should see {string} is shared in the Bazaar",
      (notebookName, context) async {},
    ),
  ];
}

Future<http.Response> httpPost(
    String uri, Object object) async {
  final response = await http.post(Uri.parse(uri),
      headers: {
        'Content-Type': 'application/json; charset=UTF-8',
      },
      body: jsonEncode(object));
  return response;
}
