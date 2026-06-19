package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotePropertyIndex;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NotePropertyIndexRepository;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
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
  @Autowired NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;

  @Test
  void run_sets_target_note_id_for_link_properties_from_wiki_title_cache() throws Exception {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note target = makeMe.aNote().title("Target").notebook(notebook).please();
    String markdown = "---\n" + "example of: \"[[Target]]\"\n" + "topic: physics\n" + "---\n\nbody";
    Note note = makeMe.aNote().notebook(notebook).content(markdown).please();

    runPropertyTrackingBackfill(makeMe.aTimestamp().of(1, 1).please());
    seedWikiTitleCacheRow(note, target, "Target");

    runTargetNoteBackfill();

    NotePropertyIndex linkRow = findIndexRow(note, "example of");
    assertThat(linkRow.getTargetNote().getId(), equalTo(target.getId()));

    NotePropertyIndex plainRow = findIndexRow(note, "topic");
    assertThat(plainRow.getTargetNote(), nullValue());
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
    seedWikiTitleCacheRow(note, target, "Target");

    runTargetNoteBackfill();
    Integer firstTargetId = findIndexRow(note, "example of").getTargetNote().getId();

    runTargetNoteBackfill();

    assertThat(findIndexRow(note, "example of").getTargetNote().getId(), equalTo(firstTargetId));
  }

  private void seedWikiTitleCacheRow(Note note, Note target, String linkText) {
    NoteWikiTitleCache row = new NoteWikiTitleCache();
    row.setNote(note);
    row.setTargetNote(target);
    row.setLinkText(linkText);
    noteWikiTitleCacheRepository.save(row);
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

  private void runTargetNoteBackfill() throws Exception {
    entityManager.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      NotePropertyIndexTargetNoteBackfill.run(connection);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }
}
