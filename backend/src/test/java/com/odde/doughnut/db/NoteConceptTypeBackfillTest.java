package com.odde.doughnut.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.services.NoteConceptTypeBackfill;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteConceptTypeBackfillTest {

  private static final String TYPELESS_NOTE = "body only";
  private static final String NOTEBOOK_README = "notebook readme";
  private static final String FOLDER_README = "folder readme";

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  Note note;
  Notebook notebook;
  Folder folder;

  @BeforeEach
  void seedTypelessNoteAndReadmes() {
    notebook = makeMe.aNotebook().readmeContent(NOTEBOOK_README).please();
    folder = makeMe.aFolder().notebook(notebook).readmeContent(FOLDER_README).please();
    note = makeMe.aNote().folder(folder).content(TYPELESS_NOTE).please();
  }

  @Test
  void defaultGateDoesNotChangeNoteContent() throws Exception {
    runBackfill("1=0");

    assertThat(rawNoteContent(note.getId()), is(TYPELESS_NOTE));
  }

  @Nested
  class WhenGateIsEnabled {
    @BeforeEach
    void backfill() throws Exception {
      runBackfill("1=1");
    }

    @Test
    void updatesNoteContent() {
      assertThat(rawNoteContent(note.getId()), is("---\ntype: Note\n---\n" + TYPELESS_NOTE));
    }

    @Test
    void leavesReadmeColumnsUntouched() {
      assertThat(rawNotebookReadme(notebook.getId()), is(NOTEBOOK_README));
      assertThat(rawFolderReadme(folder.getId()), is(FOLDER_README));
    }
  }

  private void runBackfill(String gate) throws Exception {
    makeMe.entityPersister.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      NoteConceptTypeBackfill.run(connection, gate);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private String rawNoteContent(Integer id) {
    return jdbcTemplate.queryForObject("SELECT content FROM note WHERE id = ?", String.class, id);
  }

  private String rawNotebookReadme(Integer id) {
    return jdbcTemplate.queryForObject(
        "SELECT readme_content FROM notebook WHERE id = ?", String.class, id);
  }

  private String rawFolderReadme(Integer id) {
    return jdbcTemplate.queryForObject(
        "SELECT readme_content FROM folder WHERE id = ?", String.class, id);
  }
}
