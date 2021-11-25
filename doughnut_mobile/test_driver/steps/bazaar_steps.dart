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
        final response = await http.post(Uri.parse('http://localhost:9081/api/testability/seed_notes?external_identifier=old_learner'),
          headers: {
            'Content-Type': 'application/json; charset=UTF-8',
          },
          body: jsonEncode([{'title': notebookName}]),
            );

        if (response.statusCode == 200) {
          context.expect((jsonDecode(response.body) as List).length, 1);
          return;
        }
        context.expect(response.statusCode, 200, reason: 'testability api call failed (${response.statusCode})\n${response.body}');
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
