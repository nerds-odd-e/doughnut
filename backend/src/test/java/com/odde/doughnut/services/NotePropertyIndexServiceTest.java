package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotePropertyIndex;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NotePropertyIndexRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.testability.MakeMe;
import jakarta.persistence.EntityManager;
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
class NotePropertyIndexServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired NotePropertyIndexService notePropertyIndexService;
  @Autowired NotePropertyIndexRepository notePropertyIndexRepository;
  @Autowired NoteRepository noteRepository;
  @Autowired EntityManager entityManager;

  @Nested
  class refreshForNote {

    @Test
    void indexes_content_keys_and_excludes_reserved_structural_keys() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      String markdown =
          "---\n"
              + "example of: \"[[Target]]\"\n"
              + "topic: physics\n"
              + "image: /attachments/1\n"
              + "url: https://example.com\n"
              + "---\n\nbody";
      Note note = makeMe.aNote().notebook(notebook).content(markdown).please();

      notePropertyIndexService.refreshForNote(note);

      List<String> keys =
          notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()).stream()
              .map(NotePropertyIndex::getPropertyKey)
              .toList();
      assertThat(keys, containsInAnyOrder("example of", "topic"));
    }

    @Test
    void replaces_rows_when_a_key_is_renamed() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note note = makeMe.aNote().notebook(notebook).content("---\ntopic: old\n---\n").please();

      notePropertyIndexService.refreshForNote(note);
      assertThat(notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()), hasSize(1));

      note.setContent("---\nsubject: new\n---\n");
      makeMe.entityPersister.merge(note);
      notePropertyIndexService.refreshForNote(note);

      List<String> keys =
          notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()).stream()
              .map(NotePropertyIndex::getPropertyKey)
              .toList();
      assertThat(keys, containsInAnyOrder("subject"));
    }

    @Test
    void removes_row_when_a_key_is_deleted() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note note =
          makeMe.aNote().notebook(notebook).content("---\ntopic: a\nextra: b\n---\n").please();

      notePropertyIndexService.refreshForNote(note);
      assertThat(notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()), hasSize(2));

      note.setContent("---\ntopic: a\n---\n");
      makeMe.entityPersister.merge(note);
      notePropertyIndexService.refreshForNote(note);

      List<String> keys =
          notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()).stream()
              .map(NotePropertyIndex::getPropertyKey)
              .toList();
      assertThat(keys, containsInAnyOrder("topic"));
    }

    @Test
    void leaves_no_rows_when_there_is_no_frontmatter() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note note = makeMe.aNote().notebook(notebook).content("plain body").please();

      notePropertyIndexService.refreshForNote(note);

      assertThat(notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()), empty());
    }

    @Test
    void stores_target_note_id_when_property_value_is_a_resolvable_wiki_link() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note existing = makeMe.aNote().title("Existing").notebook(notebook).please();
      Note note =
          makeMe
              .aNote()
              .notebook(notebook)
              .content("---\nexample of: \"[[Existing]]\"\n---\n")
              .please();

      notePropertyIndexService.refreshForNote(note);

      NotePropertyIndex row =
          notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()).getFirst();
      assertThat(row.getPropertyKey(), equalTo("example of"));
      assertThat(row.getTargetNote().getId(), equalTo(existing.getId()));
    }

    @Test
    void stores_null_target_when_property_value_is_not_a_wiki_link() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note note = makeMe.aNote().notebook(notebook).content("---\ntopic: physics\n---\n").please();

      notePropertyIndexService.refreshForNote(note);

      NotePropertyIndex row =
          notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()).getFirst();
      assertThat(row.getTargetNote(), nullValue());
    }

    @Test
    void stores_null_target_when_wiki_link_does_not_resolve() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note note =
          makeMe
              .aNote()
              .notebook(notebook)
              .content("---\nexample of: \"[[Missing]]\"\n---\n")
              .please();

      notePropertyIndexService.refreshForNote(note);

      NotePropertyIndex row =
          notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()).getFirst();
      assertThat(row.getTargetNote(), nullValue());
    }

    @Test
    void clears_rows_when_frontmatter_is_removed() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note note = makeMe.aNote().notebook(notebook).content("---\ntopic: a\n---\n").please();

      notePropertyIndexService.refreshForNote(note);
      assertThat(notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()), hasSize(1));

      note.setContent("plain body");
      makeMe.entityPersister.merge(note);
      notePropertyIndexService.refreshForNote(note);

      assertThat(notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()), empty());
    }
  }

  @Test
  void deleting_note_cascades_index_rows() {
    User user = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note note = makeMe.aNote().notebook(notebook).content("---\ntopic: a\n---\n").please();
    notePropertyIndexService.refreshForNote(note);
    Integer noteId = note.getId();
    assertThat(notePropertyIndexRepository.findByNote_IdOrderByIdAsc(noteId), hasSize(1));

    entityManager.flush();
    entityManager.clear();
    noteRepository.deleteById(noteId);
    entityManager.flush();

    assertThat(notePropertyIndexRepository.findByNote_IdOrderByIdAsc(noteId), empty());
  }
}
