package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.NoteAliasIndex;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteAliasIndexRepository;
import com.odde.donut.testability.MakeMe;
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

  private List<NoteAliasIndex> aliasRows(Note note) {
    return noteAliasIndexRepository.findByNote_IdOrderByIdAsc(note.getId());
  }

  @Nested
  class refreshForNote {

    @Test
    void indexes_valid_frontmatter_aliases_with_display_and_lookup_key() {
      User user = makeMe.aUser().please();
      Note note =
          makeMe
              .aNote()
              .title("colour")
              .notebookOwnedBy(user)
              .content("---\naliases:\n  - color\n  - hue\n---\n\nbody")
              .please();

      noteAliasIndexService.refreshForNote(note);

      List<NoteAliasIndex> rows = aliasRows(note);
      assertThat(rows, hasSize(2));
      assertThat(rows.get(0).getAliasDisplay(), equalTo("color"));
      assertThat(rows.get(0).getAliasLookupKey(), equalTo("color"));
      assertThat(rows.get(1).getAliasDisplay(), equalTo("hue"));
      assertThat(rows.get(1).getAliasLookupKey(), equalTo("hue"));
    }

    @Test
    void ignores_blank_invalid_and_duplicate_aliases() {
      User user = makeMe.aUser().please();
      Note note =
          makeMe
              .aNote()
              .notebookOwnedBy(user)
              .content(
                  "---\naliases:\n  - Color\n  - color\n  - \"   \"\n  - bad|alias\n---\n\nbody")
              .please();

      noteAliasIndexService.refreshForNote(note);

      List<NoteAliasIndex> rows = aliasRows(note);
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getAliasDisplay(), equalTo("Color"));
    }

    @Test
    void replaces_previous_rows_on_second_refresh() {
      User user = makeMe.aUser().please();
      Note note =
          makeMe.aNote().notebookOwnedBy(user).content("---\naliases:\n  - one\n---\n\n").please();

      noteAliasIndexService.refreshForNote(note);
      assertThat(aliasRows(note), hasSize(1));

      note.setContent("---\naliases:\n  - two\n---\n\n");
      makeMe.entityPersister.merge(note);
      noteAliasIndexService.refreshForNote(note);

      List<NoteAliasIndex> rows = aliasRows(note);
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getAliasDisplay(), equalTo("two"));
    }

    @Test
    void leaves_no_rows_when_content_has_no_aliases() {
      User user = makeMe.aUser().please();
      Note note = makeMe.aNote().notebookOwnedBy(user).content("plain body").please();

      noteAliasIndexService.refreshForNote(note);

      assertThat(aliasRows(note), empty());
    }

    @Test
    void indexes_only_plain_aliases_when_wiki_link_overlap_declared() {
      User user = makeMe.aUser().please();
      Note note =
          makeMe
              .aNote()
              .title("colour")
              .notebookOwnedBy(user)
              .content(
                  """
                  ---
                  aliases:
                    - color
                    - "[[Other Note]]"
                    - "[[Shared Notebook:Hue|display]]"
                  ---

                  body
                  """)
              .please();

      noteAliasIndexService.refreshForNote(note);

      List<NoteAliasIndex> rows = aliasRows(note);
      assertThat(rows, hasSize(1));
      assertThat(rows.get(0).getAliasDisplay(), equalTo("color"));
      assertThat(
          rows.stream().map(NoteAliasIndex::getAliasDisplay).toList(),
          everyItem(not(containsString("[["))));
    }

    @Test
    void leaves_no_rows_when_aliases_are_wiki_link_overlap_only() {
      User user = makeMe.aUser().please();
      Note note =
          makeMe
              .aNote()
              .notebookOwnedBy(user)
              .content(
                  """
                  ---
                  aliases:
                    - "[[Other Note]]"
                    - "[[Shared Notebook:Hue|display]]"
                  ---

                  body
                  """)
              .please();

      noteAliasIndexService.refreshForNote(note);

      assertThat(aliasRows(note), empty());
    }

    @Test
    void leaves_no_rows_when_aliases_list_is_empty() {
      User user = makeMe.aUser().please();
      Note note =
          makeMe.aNote().notebookOwnedBy(user).content("---\naliases: []\n---\n\nbody").please();

      noteAliasIndexService.refreshForNote(note);

      assertThat(aliasRows(note), empty());
    }

    @Test
    void removes_plain_alias_row_when_only_wiki_link_overlap_remains() {
      User user = makeMe.aUser().please();
      Note note =
          makeMe
              .aNote()
              .notebookOwnedBy(user)
              .content(
                  """
                  ---
                  aliases:
                    - color
                    - "[[Other Note]]"
                  ---

                  body
                  """)
              .please();

      noteAliasIndexService.refreshForNote(note);
      assertThat(aliasRows(note), hasSize(1));

      note.setContent(
          """
          ---
          aliases:
            - "[[Other Note]]"
          ---

          body
          """);
      makeMe.entityPersister.merge(note);
      noteAliasIndexService.refreshForNote(note);

      assertThat(aliasRows(note), empty());
    }
  }
}
