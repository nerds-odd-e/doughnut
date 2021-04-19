import 'package:doughnut_frontend/core/enums/view_state.dart';
import 'package:doughnut_frontend/core/models/note.dart';
import 'package:doughnut_frontend/core/services/api.dart';
import 'package:doughnut_frontend/helpers/dependency_assembly.dart';

import 'base_model.dart';

class BazaarNotesModel extends BaseModel {
  API api = dependencyAssembler<API>();

  List<Note> _notes = List.empty();

  List<Note> get notes {
    return _notes;
  }

  Future<List<Note>> retrieveBazaarNotes() async {
    applyState(ViewState.Busy);
    _notes = await api.fetchBazaarNotes();
    applyState(ViewState.Idle);
    return _notes;
  }
}
