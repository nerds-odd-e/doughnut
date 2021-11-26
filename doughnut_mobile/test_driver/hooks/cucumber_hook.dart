import '../steps/testability.dart';
import 'dart:async';
import 'package:gherkin/gherkin.dart';

class CucumberHook extends Hook {
  @override
  Future<void> onBeforeScenario(
      TestConfiguration config,
      String scenario,
      Iterable<Tag> tags,
      ) async {
    await TestabilityContextless().cleanDbAndResetTestabilitySettings();
  }
}