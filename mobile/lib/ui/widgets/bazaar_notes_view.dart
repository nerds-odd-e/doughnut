import 'package:doughnut_frontend/core/models/note.dart';
import 'package:doughnut_frontend/core/services/api.dart';
import 'package:doughnut_frontend/core/viewmodels/bazaar_notes_model.dart';
import 'package:doughnut_frontend/helpers/dependency_assembly.dart';
import 'package:flutter/material.dart';

class BazaarNotesView extends StatelessWidget {
  final BazaarNotesModel bazaarNotesModel = dependencyAssembler<BazaarNotesModel>();

  @override
  Widget build(BuildContext context) {
    bazaarNotesModel.api = API();
    return FutureBuilder<List<Note>>(
      future: bazaarNotesModel.retrieveBazaarNotes(),
      builder: (context, snapshot) {
        if (snapshot.hasData) {
          List<Note>? notes = bazaarNotesModel.notes;
          print(notes);
          return _bazaarNotesListView(notes);
        } else if (snapshot.hasError) {
          return Text("${snapshot.error}");
        }
        return CircularProgressIndicator();
      },
    );
  }

  ListView _bazaarNotesListView(data) {
    return ListView.builder(
        itemCount: data.length,
        itemBuilder: (context, index) {
          return _tile(
              data[index].id.toString(), data[index].skipReviewEntirely.toString(), Icons.photo_album_rounded);
        });
  }

  ListTile _tile(String noteId, String skipReviewEntirely, IconData icon) =>
      ListTile(
        title: Text(noteId,
            style: TextStyle(
              fontWeight: FontWeight.w500,
              fontSize: 20,
            )),
        subtitle: Text(skipReviewEntirely),
        leading: Icon(
          icon,
          color: Colors.blue[500],
        ),
      );
}
