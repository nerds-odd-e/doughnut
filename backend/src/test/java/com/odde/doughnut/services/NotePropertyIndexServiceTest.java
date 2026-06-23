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
    void indexes_list_property_with_one_row_per_resolved_target_preserving_yaml_order() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note targetA = makeMe.aNote().title("A").notebook(notebook).please();
      Note targetB = makeMe.aNote().title("B").notebook(notebook).please();
      String markdown =
          "---\n" + "example of:\n" + "  - \"[[A]]\"\n" + "  - \"[[B]]\"\n" + "---\n\nbody";
      Note note = makeMe.aNote().notebook(notebook).content(markdown).please();

      notePropertyIndexService.refreshForNote(note);

      List<NotePropertyIndex> rows =
          notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId());
      assertThat(rows, hasSize(2));
      assertThat(rows.get(0).getPropertyKey(), equalTo("example of"));
      assertThat(rows.get(0).getItemIndex(), equalTo(0));
      assertThat(rows.get(0).getTargetNote().getId(), equalTo(targetA.getId()));
      assertThat(rows.get(1).getPropertyKey(), equalTo("example of"));
      assertThat(rows.get(1).getItemIndex(), equalTo(1));
      assertThat(rows.get(1).getTargetNote().getId(), equalTo(targetB.getId()));
    }

    @Test
    void leaves_no_rows_for_empty_list_property() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note note = makeMe.aNote().notebook(notebook).content("---\ntopic: []\n---\n\nbody").please();

      notePropertyIndexService.refreshForNote(note);

      assertThat(notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()), empty());
    }

    @Test
    void excludes_passthrough_keys_from_indexing() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      String markdown =
          "---\n"
              + "tags:\n"
              + "  - t1\n"
              + "aliases: [a1]\n"
              + "cssclasses:\n"
              + "  - c1\n"
              + "topic: physics\n"
              + "---\n\nbody";
      Note note = makeMe.aNote().notebook(notebook).content(markdown).please();

      notePropertyIndexService.refreshForNote(note);

      List<String> keys =
          notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()).stream()
              .map(NotePropertyIndex::getPropertyKey)
              .toList();
      assertThat(keys, containsInAnyOrder("topic"));
    }

    @Test
    void keeps_exact_suffix_keys_independent() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note targetA = makeMe.aNote().title("A").notebook(notebook).please();
      Note targetB = makeMe.aNote().title("B").notebook(notebook).please();
      Note targetC = makeMe.aNote().title("C").notebook(notebook).please();
      String markdown =
          "---\n"
              + "example of:\n"
              + "  - \"[[A]]\"\n"
              + "  - \"[[B]]\"\n"
              + "example of 2: \"[[C]]\"\n"
              + "---\n\nbody";
      Note note = makeMe.aNote().notebook(notebook).content(markdown).please();

      notePropertyIndexService.refreshForNote(note);

      List<NotePropertyIndex> rows =
          notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId());
      assertThat(rows, hasSize(3));
      assertThat(
          rows.stream().filter(r -> "example of".equals(r.getPropertyKey())).count(), equalTo(2L));
      assertThat(
          rows.stream().filter(r -> "example of 2".equals(r.getPropertyKey())).count(),
          equalTo(1L));
      NotePropertyIndex suffixRow =
          rows.stream().filter(r -> "example of 2".equals(r.getPropertyKey())).findFirst().get();
      assertThat(suffixRow.getItemIndex(), equalTo(0));
      assertThat(suffixRow.getTargetNote().getId(), equalTo(targetC.getId()));
    }

    @Test
    void stores_scalar_with_item_index_zero() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note note = makeMe.aNote().notebook(notebook).content("---\ntopic: physics\n---\n").please();

      notePropertyIndexService.refreshForNote(note);

      NotePropertyIndex row =
          notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()).getFirst();
      assertThat(row.getItemIndex(), equalTo(0));
    }

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
    void indexes_non_empty_list_without_resolved_targets_as_one_null_target_row() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      String markdown =
          "---\n" + "topic:\n" + "  - alpha\n" + "  - beta\n" + "  - gamma\n" + "---\n\nbody";
      Note note = makeMe.aNote().notebook(notebook).content(markdown).please();

      notePropertyIndexService.refreshForNote(note);

      List<NotePropertyIndex> rows =
          notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId());
      assertThat(rows, hasSize(1));
      assertThat(rows.getFirst().getPropertyKey(), equalTo("topic"));
      assertThat(rows.getFirst().getItemIndex(), equalTo(0));
      assertThat(rows.getFirst().getTargetNote(), nullValue());
    }

    @Test
    void indexes_list_with_mixed_link_and_non_link_items_only_for_resolved_targets() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      Note targetA = makeMe.aNote().title("A").notebook(notebook).please();
      Note targetC = makeMe.aNote().title("C").notebook(notebook).please();
      String markdown =
          "---\n"
              + "example of:\n"
              + "  - \"[[A]]\"\n"
              + "  - plain text\n"
              + "  - \"[[C]]\"\n"
              + "---\n\nbody";
      Note note = makeMe.aNote().notebook(notebook).content(markdown).please();

      notePropertyIndexService.refreshForNote(note);

      List<NotePropertyIndex> rows =
          notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId());
      assertThat(rows, hasSize(2));
      assertThat(rows.get(0).getItemIndex(), equalTo(0));
      assertThat(rows.get(0).getTargetNote().getId(), equalTo(targetA.getId()));
      assertThat(rows.get(1).getItemIndex(), equalTo(2));
      assertThat(rows.get(1).getTargetNote().getId(), equalTo(targetC.getId()));
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
    void collapses_unresolved_list_wiki_links_to_one_null_target_row() {
      User user = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
      String markdown =
          "---\n"
              + "example of:\n"
              + "  - \"[[Missing]]\"\n"
              + "  - \"[[AlsoMissing]]\"\n"
              + "---\n\nbody";
      Note note = makeMe.aNote().notebook(notebook).content(markdown).please();

      notePropertyIndexService.refreshForNote(note);

      List<NotePropertyIndex> rows =
          notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId());
      assertThat(rows, hasSize(1));
      assertThat(rows.getFirst().getItemIndex(), equalTo(0));
      assertThat(rows.getFirst().getTargetNote(), nullValue());
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
