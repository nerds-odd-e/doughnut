package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteAliasIndex;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteAliasIndexRepository;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteAliasIndexServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired NoteAliasIndexService noteAliasIndexService;
  @Autowired NoteAliasIndexRepository noteAliasIndexRepository;

  @Nested
  class refreshForNote {

    @Test
    void indexes_valid_frontmatter_aliases_with_display_and_lookup_key() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      String markdown = "---\naliases:\n  - color\n  - hue\n---\n\nbody";
      Note note = makeMe.aNote().title("colour").notebook(notebook).content(markdown).please();

      noteAliasIndexService.refreshForNote(note);

      List<NoteAliasIndex> rows = noteAliasIndexRepository.findByNote_IdOrderByIdAsc(note.getId());
      assertThat(rows, hasSize(2));
      assertThat(rows.get(0).getAliasDisplay(), equalTo("color"));
      assertThat(rows.get(0).getAliasLookupKey(), equalTo("color"));
      assertThat(rows.get(1).getAliasDisplay(), equalTo("hue"));
      assertThat(rows.get(1).getAliasLookupKey(), equalTo("hue"));
    }

    @Test
    void ignores_blank_invalid_and_duplicate_aliases() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      String markdown =
          "---\naliases:\n  - Color\n  - color\n  - \"   \"\n  - bad|alias\n---\n\nbody";
      Note note = makeMe.aNote().notebook(notebook).content(markdown).please();

      noteAliasIndexService.refreshForNote(note);

      List<NoteAliasIndex> rows = noteAliasIndexRepository.findByNote_IdOrderByIdAsc(note.getId());
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getAliasDisplay(), equalTo("Color"));
    }

    @Test
    void replaces_previous_rows_on_second_refresh() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note note =
          makeMe.aNote().notebook(notebook).content("---\naliases:\n  - one\n---\n\n").please();

      noteAliasIndexService.refreshForNote(note);
      assertThat(noteAliasIndexRepository.findByNote_IdOrderByIdAsc(note.getId()), hasSize(1));

      note.setContent("---\naliases:\n  - two\n---\n\n");
      makeMe.entityPersister.merge(note);
      noteAliasIndexService.refreshForNote(note);

      List<NoteAliasIndex> rows = noteAliasIndexRepository.findByNote_IdOrderByIdAsc(note.getId());
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getAliasDisplay(), equalTo("two"));
    }

    @Test
    void leaves_no_rows_when_content_has_no_aliases() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note note = makeMe.aNote().notebook(notebook).content("plain body").please();

      noteAliasIndexService.refreshForNote(note);

      assertThat(noteAliasIndexRepository.findByNote_IdOrderByIdAsc(note.getId()), empty());
    }
  }
}
