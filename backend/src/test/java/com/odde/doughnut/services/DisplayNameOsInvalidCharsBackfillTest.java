package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Connection;
import java.util.Optional;
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
class DisplayNameOsInvalidCharsBackfillTest {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired WikiLinkResolver wikiLinkResolver;

  @Test
  void run_convertsIllegalCharsOnNotesFoldersAndNotebooksKeepingSiblingsDistinct()
      throws Exception {
    Notebook notebook = makeMe.aNotebook().name("Nb").please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Food").please();
    Note recipe = makeMe.aNote().folder(folder).title("Recipe").please();
    Note recipeStar = makeMe.aNote().folder(folder).title("Star").please();

    jdbcTemplate.update("UPDATE notebook SET name = ? WHERE id = ?", "Nb*", notebook.getId());
    jdbcTemplate.update("UPDATE folder SET name = ? WHERE id = ?", "Food*", folder.getId());
    jdbcTemplate.update("UPDATE note SET title = ? WHERE id = ?", "Recipe*", recipeStar.getId());

    runBackfill();

    assertThat(rawNotebookName(notebook.getId()), is("Nb＊"));
    assertThat(rawFolderName(folder.getId()), is("Food＊"));
    assertThat(rawNoteTitle(recipe.getId()), is("Recipe"));
    assertThat(rawNoteTitle(recipeStar.getId()), is("Recipe＊"));
  }

  @Test
  void run_rewritesInboundWikiAndPathMarkdownTokensToConvertedSpellings() throws Exception {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).name("Nb").please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Food").please();
    Note recipeStar = makeMe.aNote().folder(folder).title("Star").please();
    Note carrier = makeMe.aNote().folder(folder).title("Carrier").content("See [[Star]].").please();

    jdbcTemplate.update("UPDATE notebook SET name = ? WHERE id = ?", "Nb*", notebook.getId());
    jdbcTemplate.update("UPDATE folder SET name = ? WHERE id = ?", "Food*", folder.getId());
    jdbcTemplate.update("UPDATE note SET title = ? WHERE id = ?", "Recipe*", recipeStar.getId());
    jdbcTemplate.update(
        "UPDATE note SET content = ? WHERE id = ?",
        "See [[Recipe*]] and [x](/Food*/Recipe*.md) and [[Nb*:Recipe*]].",
        carrier.getId());

    runBackfill();

    assertThat(
        rawNoteContent(carrier.getId()),
        is("See [[Recipe＊]] and [x](/Food＊/Recipe＊.md) and [[Nb＊:Recipe＊]]."));

    makeMe.refresh(carrier);
    makeMe.refresh(recipeStar);
    makeMe.refresh(notebook);
    Optional<Note> resolved = wikiLinkResolver.resolveWikiLinkToken("Recipe＊", carrier, owner);
    assertThat(resolved.isPresent(), is(true));
    assertThat(resolved.get().getId(), is(recipeStar.getId()));
  }

  @Test
  void run_failsLoudWhenConvertWouldCollideWithSiblingNoteTitle() throws Exception {
    Notebook notebook = makeMe.aNotebook().please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Scope").please();
    Note illegal = makeMe.aNote().folder(folder).title("Illegal").please();
    Note fullwidth = makeMe.aNote().folder(folder).title("Recipe＊").please();

    jdbcTemplate.update("UPDATE note SET title = ? WHERE id = ?", "Recipe*", illegal.getId());

    IllegalStateException thrown = assertThrows(IllegalStateException.class, this::runBackfill);

    assertThat(thrown.getMessage(), containsString("note"));
    assertThat(thrown.getMessage(), containsString("uk_note_notebook_folder_title"));
    assertThat(thrown.getMessage(), containsString(String.valueOf(illegal.getId())));
    assertThat(thrown.getMessage(), containsString(String.valueOf(fullwidth.getId())));
    assertThat(rawNoteTitle(illegal.getId()), is("Recipe*"));
  }

  @Test
  void run_failsLoudWhenConvertWouldLeaveABlankName() throws Exception {
    Notebook notebook = makeMe.aNotebook().please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Scope").please();
    Note blanking = makeMe.aNote().folder(folder).title("Blanking").please();

    jdbcTemplate.update("UPDATE note SET title = ? WHERE id = ?", "\u0001", blanking.getId());

    IllegalStateException thrown = assertThrows(IllegalStateException.class, this::runBackfill);

    assertThat(thrown.getMessage(), containsString("note"));
    assertThat(thrown.getMessage(), containsString(String.valueOf(blanking.getId())));
    assertThat(rawNoteTitle(blanking.getId()), is("\u0001"));
  }

  private void runBackfill() throws Exception {
    makeMe.entityPersister.flush();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      DisplayNameOsInvalidCharsBackfill.run(connection);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private String rawNoteTitle(Integer id) {
    return jdbcTemplate.queryForObject("SELECT title FROM note WHERE id = ?", String.class, id);
  }

  private String rawNoteContent(Integer id) {
    return jdbcTemplate.queryForObject("SELECT content FROM note WHERE id = ?", String.class, id);
  }

  private String rawFolderName(Integer id) {
    return jdbcTemplate.queryForObject("SELECT name FROM folder WHERE id = ?", String.class, id);
  }

  private String rawNotebookName(Integer id) {
    return jdbcTemplate.queryForObject("SELECT name FROM notebook WHERE id = ?", String.class, id);
  }
}
