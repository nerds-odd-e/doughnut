import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:doughnut_frontend/core/models/note.dart';

class API {
  static const endpoint = "https://doughnut.odd-e.com/api/bazaar_notes";

  var client = new http.Client();

  Future<List<Note>> getBazaarNotes() async {
    List<Note> notes = List.empty();
    var response = await client.get(Uri.parse(endpoint));

    var data = json.decode(response.body) as List<dynamic>;

    for (var note in data) {
      notes.add(Note.fromJson(note));
    }

    return notes;
  }
}
