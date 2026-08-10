package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Connection;
import javax.sql.DataSource;
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
class DisplayNameSurroundingWhitespaceBackfillTest {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void run_trimsSurroundingWhitespaceOnNotesFoldersAndNotebooks() throws Exception {
    Notebook notebook = makeMe.aNotebook().name("Keep").please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("KeepFolder").please();
    Note note = makeMe.aNote().folder(folder).title("KeepNote").please();

    jdbcTemplate.update(
        "UPDATE notebook SET name = ? WHERE id = ?", "  Notebook\u3000", notebook.getId());
    jdbcTemplate.update(
        "UPDATE folder SET name = ? WHERE id = ?", "\u3000Folder  ", folder.getId());
    jdbcTemplate.update("UPDATE note SET title = ? WHERE id = ?", "  Title\u3000", note.getId());

    runBackfill();

    assertThat(rawNotebookName(notebook.getId()), is("Notebook"));
    assertThat(rawFolderName(folder.getId()), is("Folder"));
    assertThat(rawNoteTitle(note.getId()), is("Title"));
  }

  @Test
  void run_failsLoudWhenTrimWouldCollideWithSiblingNoteTitle() throws Exception {
    Notebook notebook = makeMe.aNotebook().please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Scope").please();
    Note padded = makeMe.aNote().folder(folder).title("Padded").please();
    Note clean = makeMe.aNote().folder(folder).title("A").please();

    jdbcTemplate.update("UPDATE note SET title = ? WHERE id = ?", "  A", padded.getId());

    IllegalStateException thrown = assertThrows(IllegalStateException.class, this::runBackfill);

    assertThat(thrown.getMessage(), containsString("note"));
    assertThat(thrown.getMessage(), containsString("uk_note_notebook_folder_title"));
    assertThat(thrown.getMessage(), containsString(String.valueOf(padded.getId())));
    assertThat(thrown.getMessage(), containsString(String.valueOf(clean.getId())));
    assertThat(rawNoteTitle(padded.getId()), is("  A"));
  }

  private void runBackfill() throws Exception {
    makeMe.entityPersister.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      DisplayNameSurroundingWhitespaceBackfill.run(connection);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private String rawNoteTitle(Integer id) {
    return jdbcTemplate.queryForObject("SELECT title FROM note WHERE id = ?", String.class, id);
  }

  private String rawFolderName(Integer id) {
    return jdbcTemplate.queryForObject("SELECT name FROM folder WHERE id = ?", String.class, id);
  }

  private String rawNotebookName(Integer id) {
    return jdbcTemplate.queryForObject("SELECT name FROM notebook WHERE id = ?", String.class, id);
  }
}
