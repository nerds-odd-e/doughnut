import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:patrol/patrol.dart';

import 'testability.dart';

void main() {
  patrolTest(
    'a Scenario',
    ($) async {
      final testability = Testability();
      await testability.cleanDbAndResetTestabilitySettings();

      // App is already launched by Patrol CLI; wait for UI to settle.
      await $.pumpAndSettle();

      final locator = $(const Key('xxx'));
      for (var i = 0; i < 3; i += 1) {
        if (locator.exists) {
          await locator.tap();
        }
      }
    },
  );
}
