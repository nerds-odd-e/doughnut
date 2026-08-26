package com.odde.donut.testability;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteRealmService;
import com.odde.donut.testability.model.NotesTestData;
import com.odde.donut.testability.model.NotesTestData.NoteTestData;
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
  @Autowired AuthorizationService authorizationService;
  @Autowired NoteRealmService noteRealmService;

  @Test
  void injectNotes_assignsExplicitFolderPathsForNestedNotes() {
    var user = makeMe.aUser().please();
    var data = new NotesTestData();
    data.setNotebookName("Folder inject nb");
    data.setExternalIdentifier(user.getExternalIdentifier());

    List<NoteTestData> rows = new ArrayList<>();
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
    var data = new NotesTestData();
    data.setNotebookName("Atlas nb");
    data.setExternalIdentifier(user.getExternalIdentifier());

    List<NoteTestData> rows = new ArrayList<>();
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

  @Test
  void injectNotes_singleNotebookRootNoteIsReadableByOwner()
      throws UnexpectedNoAccessRightException {
    User user = makeMe.aUser().please();
    var data = new NotesTestData();
    data.setNotebookName("90s hits");
    data.setExternalIdentifier(user.getExternalIdentifier());
    List<NoteTestData> rows = new ArrayList<>();
    rows.add(row("Who Let the Dogs Out", null));
    data.setNoteTestData(rows);

    Map<String, Integer> ids = testabilityRestController.injectNotes(data);
    Note note = noteRepository.findById(ids.get("Who Let the Dogs Out")).orElseThrow();

    authorizationService.assertReadAuthorization(user, note);
    NoteRealm realm = noteRealmService.build(note, user);
    assertThat(realm.getId(), equalTo(note.getId()));
  }

  private static NoteTestData row(String title, String folder) {
    NoteTestData n = new NoteTestData();
    n.title = title;
    n.setFolder(folder);
    return n;
  }
}
