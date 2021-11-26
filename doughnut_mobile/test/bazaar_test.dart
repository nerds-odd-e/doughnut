import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:doughnut_mobile/main.dart';

void main() {
  testWidgets('Bazaar ui', (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp());

    await tester.pump();

    expect(find.text('Shape'), findsOneWidget);
  });
}
