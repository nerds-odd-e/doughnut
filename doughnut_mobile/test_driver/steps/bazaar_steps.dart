import 'package:flutter_gherkin/flutter_gherkin.dart';
import 'package:flutter_driver/flutter_driver.dart';
import 'package:gherkin/gherkin.dart';
import './testability.dart';

List<StepDefinitionGeneric> bazaarSteps() {
  return [
    when1<String, FlutterWorld>(
      'there is a notebook {string} in the bazaar',
      (notebookName, context) async {
        await Testability(context).seedNotebookInBazaar(notebookName);
      },
    ),
    given<FlutterWorld>(
      "I haven't login",
      (context) async {},
    ),
    then1<String, FlutterWorld>(
      "I should see {string} is shared in the Bazaar",
      (notebookName, context) async {
        final isPresent = await FlutterDriverUtils.isPresent(
          context.world.driver,
          find.text(notebookName),
        );
        context.expect(isPresent, true);
      },
    ),
  ];
}

