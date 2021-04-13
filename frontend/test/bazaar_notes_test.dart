import 'package:doughnut_frontend/core/models/note.dart';
import 'package:doughnut_frontend/core/services/api.dart';
import 'package:doughnut_frontend/core/viewmodels/bazaar_notes_model.dart';
import 'package:doughnut_frontend/helpers/dependency_assembly.dart';
import 'package:flutter_test/flutter_test.dart';

class MockAPI extends API {
  @override
  Future<List<Note>> getBazaarNotes() {
    return Future.value([
      Note(id: 1, skipReviewEntirely: true),
      Note(id: 2, skipReviewEntirely: false)
    ]);
  }
}

void main() {
  setupDependencyAssembler();
  var bazaarNotesViewModel = dependencyAssembler<BazaarNotesModel>();
  bazaarNotesViewModel.api = MockAPI();

  group('Backend /api/bazaar_notes API request/response', () {
    test('Page should load a list of public notes from Bazaar', () async {
      await bazaarNotesViewModel.getBazaarNotes();
      expect(bazaarNotesViewModel.notes.length, 2);
      expect(bazaarNotesViewModel.notes[0].id, 1);
      expect(bazaarNotesViewModel.notes[0].skipReviewEntirely, true);
      expect(bazaarNotesViewModel.notes[1].id, 2);
      expect(bazaarNotesViewModel.notes[1].skipReviewEntirely, false);
    });
  });
}
