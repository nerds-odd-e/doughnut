import 'package:flutter_driver/flutter_driver.dart';
import 'package:flutter_gherkin/flutter_gherkin.dart';
import 'package:gherkin/gherkin.dart';

StepDefinitionGeneric MySteps() {
  return when<FlutterWorld>(
    'this step exists',
        (context) async {
      final locator = find.byValueKey('xxx');
      for (var i = 0; i < 3; i += 1) {
        await FlutterDriverUtils.tap(context.world.driver, locator);
      }
    },
  );
}
