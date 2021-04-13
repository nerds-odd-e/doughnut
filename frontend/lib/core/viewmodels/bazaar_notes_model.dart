import 'package:doughnut_frontend/core/enums/view_state.dart';
import 'package:doughnut_frontend/core/models/note.dart';
import 'package:doughnut_frontend/core/services/api.dart';
import 'package:doughnut_frontend/helpers/dependency_assembly.dart';

import 'base_model.dart';

class BazaarNotesModel extends BaseModel {
  API api = dependencyAssembler<API>();

  late List<Note> _notes;

  List<Note> get notes {
    return _notes;
  }

  Future getBazaarNotes() async {
    applyState(ViewState.Busy);
    _notes = await api.getBazaarNotes();
    applyState(ViewState.Idle);
  }
}
