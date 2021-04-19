import 'package:get_it/get_it.dart';
import 'package:doughnut_frontend/core/services/api.dart';
import 'package:doughnut_frontend/core/viewmodels/bazaar_notes_model.dart';

GetIt dependencyAssembler = GetIt.instance;

void setupDependencyAssembler() {
  dependencyAssembler.registerLazySingleton(() => API());
  dependencyAssembler.registerFactory(() => BazaarNotesModel());
}
