import 'package:doughnut_mobile/rest_api.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('api test', (WidgetTester tester) async {
    final restApi = RestApi();
    var bazaarNotebooks = await restApi.getBazaarNotebooks();
    expect(bazaarNotebooks, 'Shape');
  });
}
