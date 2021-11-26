import 'package:flutter_test/flutter_test.dart';

import 'package:doughnut_mobile/bazaar_widget.dart';

void main() {
  testWidgets('Bazaar ui', (WidgetTester tester) async {
    await tester.pumpWidget(BazaarWidget(Future.value("Bazaar article")));
    await tester.pump();
    expect(find.text('Bazaar article'), findsOneWidget);
  });
}
