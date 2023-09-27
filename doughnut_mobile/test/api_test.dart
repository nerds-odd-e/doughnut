import 'package:doughnut_mobile/rest_api.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart';
import 'package:http/testing.dart';

void main() {
  testWidgets('api test', (WidgetTester tester) async {
    fn(request) async {
      return Response("", 200);
    }
    final restApi = RestApi(MockClient(fn));
    var bazaarNotebooks = await restApi.getBazaarNotebooks();
    expect(bazaarNotebooks, 'Shape');
  });
}
