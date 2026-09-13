package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.ControllerTestBase;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FolderConstructionServiceTest extends ControllerTestBase {

  @Autowired private FolderConstructionService folderConstructionService;
  @Autowired private FolderRepository folderRepository;

  @Test
  void mirrorsOnlyMissingOriginalParentsUnderAnExistingCaseInsensitiveTrashRoot() {
    Notebook notebook = makeMe.aNotebook().please();
    Folder sourceRoot = makeMe.aFolder().notebook(notebook).name("Research").please();
    Folder sourceParent = makeMe.aFolder().parentFolder(sourceRoot).name("Physics").please();
    Note note = makeMe.aNote().folder(sourceParent).please();
    Folder trash = makeMe.aFolder().notebook(notebook).name("_TrAsH").please();
    Folder mirroredRoot = makeMe.aFolder().parentFolder(trash).name("Research").please();

    Folder destination = folderConstructionService.ensureTrashParentFor(note);
    Folder repeatedDestination = folderConstructionService.ensureTrashParentFor(note);

    assertThat(destination.getName(), equalTo("Physics"));
    assertThat(destination.getParentFolder().getId(), equalTo(mirroredRoot.getId()));
    assertThat(repeatedDestination.getId(), equalTo(destination.getId()));
    assertThat(folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(5));
  }
}
