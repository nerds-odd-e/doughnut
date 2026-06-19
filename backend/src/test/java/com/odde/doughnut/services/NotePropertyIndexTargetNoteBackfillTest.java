package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotePropertyIndex;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NotePropertyIndexRepository;
import com.odde.doughnut.testability.MakeMe;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotePropertyIndexTargetNoteBackfillTest {

  @Autowired MakeMe makeMe;
  @Autowired EntityManager entityManager;
  @Autowired DataSource dataSource;
  @Autowired NotePropertyIndexRepository notePropertyIndexRepository;
  @Autowired NotePropertyIndexService notePropertyIndexService;

  @Test
  void run_sets_target_note_id_for_link_properties_from_note_table() throws Exception {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note target = makeMe.aNote().title("Target").notebook(notebook).please();
    String markdown = "---\n" + "example of: \"[[Target]]\"\n" + "topic: physics\n" + "---\n\nbody";
    Note note = makeMe.aNote().notebook(notebook).content(markdown).please();

    runPropertyTrackingBackfill(makeMe.aTimestamp().of(1, 1).please());
    runTargetNoteResolutionBackfill();

    NotePropertyIndex linkRow = findIndexRow(note, "example of");
    assertThat(linkRow.getTargetNote().getId(), equalTo(target.getId()));
    assertMatchesLiveRefresh(note, "example of", target.getId());

    NotePropertyIndex plainRow = findIndexRow(note, "topic");
    assertThat(plainRow.getTargetNote(), nullValue());
  }

  @Test
  void run_resolves_cross_notebook_qualified_links() throws Exception {
    User owner = makeMe.aUser().please();
    Notebook otherNotebook =
        makeMe.aNotebook().creatorAndOwner(owner).name("Other Notebook").please();
    Note target = makeMe.aNote().title("Word").notebook(otherNotebook).please();
    Notebook ownNotebook = makeMe.aNotebook().creatorAndOwner(owner).name("My Notebook").please();
    Note carrier =
        makeMe
            .aNote()
            .notebook(ownNotebook)
            .content("---\nexample of: \"[[Other Notebook:Word]]\"\n---\n")
            .please();

    runPropertyTrackingBackfill(makeMe.aTimestamp().of(1, 1).please());
    runTargetNoteResolutionBackfill();

    assertThat(
        findIndexRow(carrier, "example of").getTargetNote().getId(), equalTo(target.getId()));
    assertMatchesLiveRefresh(carrier, "example of", target.getId());
  }

  @Test
  void run_resolves_target_in_notebook_owner_cannot_read() throws Exception {
    User owner = makeMe.aUser().please();
    User otherOwner = makeMe.aUser().please();
    Notebook secretNotebook =
        makeMe.aNotebook().creatorAndOwner(otherOwner).name("Secret Notebook").please();
    Note target = makeMe.aNote().title("Word").notebook(secretNotebook).please();
    Notebook ownNotebook = makeMe.aNotebook().creatorAndOwner(owner).name("My Notebook").please();
    Note carrier =
        makeMe
            .aNote()
            .notebook(ownNotebook)
            .content("---\nexample of: \"[[Secret Notebook:Word]]\"\n---\n")
            .please();

    runPropertyTrackingBackfill(makeMe.aTimestamp().of(1, 1).please());
    runTargetNoteResolutionBackfill();

    assertThat(
        findIndexRow(carrier, "example of").getTargetNote().getId(), equalTo(target.getId()));
    assertMatchesLiveRefresh(carrier, "example of", target.getId());
  }

  @Test
  void run_is_idempotent_when_reapplied() throws Exception {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note target = makeMe.aNote().title("Target").notebook(notebook).please();
    Note note =
        makeMe
            .aNote()
            .notebook(notebook)
            .content("---\nexample of: \"[[Target]]\"\n---\n")
            .please();

    runPropertyTrackingBackfill(makeMe.aTimestamp().of(2, 2).please());
    runTargetNoteResolutionBackfill();
    Integer firstTargetId = findIndexRow(note, "example of").getTargetNote().getId();

    runTargetNoteResolutionBackfill();

    assertThat(findIndexRow(note, "example of").getTargetNote().getId(), equalTo(firstTargetId));
    assertThat(firstTargetId, equalTo(target.getId()));
  }

  private void assertMatchesLiveRefresh(Note note, String propertyKey, Integer expectedTargetId) {
    notePropertyIndexService.refreshForNote(note);
    NotePropertyIndex refreshed = findIndexRow(note, propertyKey);
    assertThat(refreshed.getTargetNote().getId(), equalTo(expectedTargetId));
  }

  private NotePropertyIndex findIndexRow(Note note, String propertyKey) {
    return notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()).stream()
        .filter(row -> propertyKey.equals(row.getPropertyKey()))
        .findFirst()
        .orElseThrow();
  }

  private void runPropertyTrackingBackfill(Timestamp now) throws Exception {
    entityManager.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      NotePropertyTrackingBackfill.run(connection, now);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private void runTargetNoteResolutionBackfill() throws Exception {
    entityManager.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      NotePropertyIndexTargetNoteResolutionBackfill.run(connection);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }
}
