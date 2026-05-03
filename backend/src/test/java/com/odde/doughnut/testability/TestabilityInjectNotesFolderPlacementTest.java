package com.odde.doughnut.testability;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

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
  void injectNotes_assignsExplicitFolderPathsForNestedNotes() {
    var user = makeMe.aUser().please();
    var data = new TestabilityRestController.NotesTestData();
    data.setNotebookName("Folder inject nb");
    data.setExternalIdentifier(user.getExternalIdentifier());

    List<TestabilityRestController.NoteTestData> rows = new ArrayList<>();
    rows.add(row("LeSS in Action", null));
    rows.add(row("TDD", "LeSS in Action"));
    rows.add(row("TPP", "LeSS in Action/TDD"));
    rows.add(row("Const", "LeSS in Action/TPP"));
    rows.add(row("UnderTdd", "LeSS in Action/TDD"));

    data.setNoteTestData(rows);

    Map<String, Integer> ids = testabilityRestController.injectNotes(data);

    Note constNote = noteRepository.findById(ids.get("Const")).orElseThrow();
    assertThat(constNote.getFolder(), notNullValue());
    assertThat(constNote.getFolder().getName(), equalTo("TPP"));
    assertThat(constNote.getFolder().getParentFolder().getName(), equalTo("LeSS in Action"));

    Note underTdd = noteRepository.findById(ids.get("UnderTdd")).orElseThrow();
    assertThat(underTdd.getFolder(), notNullValue());
    assertThat(underTdd.getFolder().getName(), equalTo("TDD"));
    assertThat(underTdd.getFolder().getParentFolder().getName(), equalTo("LeSS in Action"));
  }

  @Test
  void injectNotes_folderSegmentNeedNotMatchAParentNoteSameTitle() {
    var user = makeMe.aUser().please();
    var data = new TestabilityRestController.NotesTestData();
    data.setNotebookName("Atlas nb");
    data.setExternalIdentifier(user.getExternalIdentifier());

    List<TestabilityRestController.NoteTestData> rows = new ArrayList<>();
    rows.add(row("Germany", "World"));
    rows.add(row("Japan", "World"));
    data.setNoteTestData(rows);

    Map<String, Integer> ids = testabilityRestController.injectNotes(data);

    Note germany = noteRepository.findById(ids.get("Germany")).orElseThrow();
    Note japan = noteRepository.findById(ids.get("Japan")).orElseThrow();
    assertThat(germany.getFolder(), notNullValue());
    assertThat(japan.getFolder(), notNullValue());
    assertThat(japan.getFolder().getId(), equalTo(germany.getFolder().getId()));
    assertThat(germany.getFolder().getName(), equalTo("World"));
  }

  private static TestabilityRestController.NoteTestData row(String title, String folder) {
    TestabilityRestController.NoteTestData n = new TestabilityRestController.NoteTestData();
    n.title = title;
    n.setFolder(folder);
    return n;
  }
}
