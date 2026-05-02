package com.odde.doughnut.testability;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TestabilityInjectNotesFolderPlacementTest {

  @Autowired TestabilityRestController testabilityRestController;
  @Autowired NoteRepository noteRepository;
  @Autowired MakeMe makeMe;

  @Test
  void injectNotes_assignsExplicitFolderIndependentOfParentAndSkipsImplicitFolders() {
    var user = makeMe.aUser().please();
    var data = new TestabilityRestController.NotesTestData();
    data.setNotebookName("Folder inject nb");
    data.setExternalIdentifier(user.getExternalIdentifier());

    List<TestabilityRestController.NoteTestData> rows = new ArrayList<>();
    rows.add(row("LeSS in Action", null, null));
    rows.add(row("TDD", null, "LeSS in Action"));
    rows.add(row("TPP", null, "LeSS in Action/TDD"));
    rows.add(row("Const", null, "LeSS in Action/TPP"));
    rows.add(row("NoFolderChild", "TDD", null));

    data.setNoteTestData(rows);

    Map<String, Integer> ids = testabilityRestController.injectNotes(data);

    Note constNote = noteRepository.findById(ids.get("Const")).orElseThrow();
    assertThat(constNote.getParent().getTitle(), equalTo("TPP"));
    assertThat(constNote.getFolder(), notNullValue());
    assertThat(constNote.getFolder().getName(), equalTo("TPP"));
    assertThat(constNote.getFolder().getParentFolder().getName(), equalTo("LeSS in Action"));

    Note noFolder = noteRepository.findById(ids.get("NoFolderChild")).orElseThrow();
    assertThat(noFolder.getFolder(), nullValue());
  }

  private static TestabilityRestController.NoteTestData row(
      String title, String parentTitle, String folder) {
    TestabilityRestController.NoteTestData n = new TestabilityRestController.NoteTestData();
    n.title = title;
    n.setParentTitle(parentTitle);
    n.setFolder(folder);
    return n;
  }
}
