import 'package:flutter_test/flutter_test.dart';
import 'package:patrol/patrol.dart';

import 'testability.dart';

void main() {
  patrolTest(
    'Bazaar browsing - as non-user',
    ($) async {
      final testability = Testability();
      await testability.cleanDbAndResetTestabilitySettings();
      await testability.seedNotebookInBazaar('Shape');

      // App is already launched by Patrol CLI; wait for UI to settle.
      await $.pumpAndSettle();

      expect($('Shape'), findsOneWidget);
    },
  );
}
