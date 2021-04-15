import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:doughnut_frontend/core/models/note.dart';

class API {
  static const endpoint = "https://doughnut.odd-e.com/api/bazaar_notes";

  var client = new http.Client();

  Future<List<Note>> fetchBazaarNotes() async {
    final response = await client.get(Uri.parse(endpoint));

    if (response.statusCode == 200) {
      List jsonResponse = json.decode(response.body);
      return jsonResponse.map((note) => new Note.fromJson(note)).toList();
    } else {
      throw Exception('Failed to fetch Bazaar Notes from API');
    }
  }
}
