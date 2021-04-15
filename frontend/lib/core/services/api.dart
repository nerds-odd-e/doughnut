import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:doughnut_frontend/helpers/constants.dart';
import 'package:doughnut_frontend/core/models/note.dart';

class API {
  static const bazaarNotesEndpoint = URL.DoughnutBazaarNotesAPIUrl;

  var client = new http.Client();

  Future<List<Note>> fetchBazaarNotes() async {
    final response = await client.get(Uri.parse(bazaarNotesEndpoint));

    if (response.statusCode == 200) {
      List jsonResponse = json.decode(response.body);
      return jsonResponse.map((note) => new Note.fromJson(note)).toList();
    } else {
      throw Exception('Failed to fetch Bazaar Notes from API');
    }
  }
}
