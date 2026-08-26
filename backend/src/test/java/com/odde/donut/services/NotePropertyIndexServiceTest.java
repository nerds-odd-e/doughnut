package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotePropertyIndex;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.testability.MakeMe;
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

  private List<NotePropertyIndex> propertyRows(Note note) {
    return notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId());
  }

  @Nested
  class refreshForNote {

    @Test
    void indexes_list_property_with_one_row_per_resolved_target_preserving_yaml_order() {
      User user = makeMe.aUser().please();
      Note targetA = makeMe.aNote().title("A").notebookOwnedBy(user).please();
      Note targetB = makeMe.aNote().title("B").underSameNotebookAs(targetA).please();
      String markdown =
          "---\n" + "example of:\n" + "  - \"[[A]]\"\n" + "  - \"[[B]]\"\n" + "---\n\nbody";
      Note note = makeMe.aNote().underSameNotebookAs(targetA).content(markdown).please();

      notePropertyIndexService.refreshForNote(note);

      List<NotePropertyIndex> rows = propertyRows(note);
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
      Note note =
          makeMe.aNote().notebookOwnedBy(user).content("---\ntopic: []\n---\n\nbody").please();

      notePropertyIndexService.refreshForNote(note);

      assertThat(propertyRows(note), empty());
    }

    @Test
    void excludes_passthrough_keys_from_indexing() {
      User user = makeMe.aUser().please();
      String markdown =
          "---\n"
              + "tags:\n"
              + "  - t1\n"
              + "aliases: [a1]\n"
              + "cssclasses:\n"
              + "  - c1\n"
              + "topic: physics\n"
              + "---\n\nbody";
      Note note = makeMe.aNote().notebookOwnedBy(user).content(markdown).please();

      notePropertyIndexService.refreshForNote(note);

      List<String> keys =
          propertyRows(note).stream().map(NotePropertyIndex::getPropertyKey).toList();
      assertThat(keys, containsInAnyOrder("topic"));
    }

    @Test
    void keeps_exact_suffix_keys_independent() {
      User user = makeMe.aUser().please();
      Note targetA = makeMe.aNote().title("A").notebookOwnedBy(user).please();
      Note targetB = makeMe.aNote().title("B").underSameNotebookAs(targetA).please();
      Note targetC = makeMe.aNote().title("C").underSameNotebookAs(targetA).please();
      String markdown =
          "---\n"
              + "example of:\n"
              + "  - \"[[A]]\"\n"
              + "  - \"[[B]]\"\n"
              + "example of 2: \"[[C]]\"\n"
              + "---\n\nbody";
      Note note = makeMe.aNote().underSameNotebookAs(targetA).content(markdown).please();

      notePropertyIndexService.refreshForNote(note);

      List<NotePropertyIndex> rows = propertyRows(note);
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
      Note note =
          makeMe.aNote().notebookOwnedBy(user).content("---\ntopic: physics\n---\n").please();

      notePropertyIndexService.refreshForNote(note);

      assertThat(propertyRows(note).getFirst().getItemIndex(), equalTo(0));
    }

    @Test
    void indexes_content_keys_and_excludes_reserved_structural_keys() {
      User user = makeMe.aUser().please();
      String markdown =
          "---\n"
              + "example of: \"[[Target]]\"\n"
              + "topic: physics\n"
              + "image: /attachments/1\n"
              + "url: https://example.com\n"
              + "---\n\nbody";
      Note note = makeMe.aNote().notebookOwnedBy(user).content(markdown).please();

      notePropertyIndexService.refreshForNote(note);

      List<String> keys =
          propertyRows(note).stream().map(NotePropertyIndex::getPropertyKey).toList();
      assertThat(keys, containsInAnyOrder("example of", "topic"));
    }

    @Test
    void replaces_rows_when_a_key_is_renamed() {
      User user = makeMe.aUser().please();
      Note note = makeMe.aNote().notebookOwnedBy(user).content("---\ntopic: old\n---\n").please();

      notePropertyIndexService.refreshForNote(note);
      assertThat(propertyRows(note), hasSize(1));

      note.setContent("---\nsubject: new\n---\n");
      makeMe.entityPersister.merge(note);
      notePropertyIndexService.refreshForNote(note);

      List<String> keys =
          propertyRows(note).stream().map(NotePropertyIndex::getPropertyKey).toList();
      assertThat(keys, containsInAnyOrder("subject"));
    }

    @Test
    void removes_row_when_a_key_is_deleted() {
      User user = makeMe.aUser().please();
      Note note =
          makeMe.aNote().notebookOwnedBy(user).content("---\ntopic: a\nextra: b\n---\n").please();

      notePropertyIndexService.refreshForNote(note);
      assertThat(propertyRows(note), hasSize(2));

      note.setContent("---\ntopic: a\n---\n");
      makeMe.entityPersister.merge(note);
      notePropertyIndexService.refreshForNote(note);

      List<String> keys =
          propertyRows(note).stream().map(NotePropertyIndex::getPropertyKey).toList();
      assertThat(keys, containsInAnyOrder("topic"));
    }

    @Test
    void leaves_no_rows_when_there_is_no_frontmatter() {
      User user = makeMe.aUser().please();
      Note note = makeMe.aNote().notebookOwnedBy(user).content("plain body").please();

      notePropertyIndexService.refreshForNote(note);

      assertThat(propertyRows(note), empty());
    }

    @Test
    void clears_rows_when_frontmatter_is_removed() {
      User user = makeMe.aUser().please();
      Note note = makeMe.aNote().notebookOwnedBy(user).content("---\ntopic: a\n---\n").please();

      notePropertyIndexService.refreshForNote(note);
      assertThat(propertyRows(note), hasSize(1));

      note.setContent("plain body");
      makeMe.entityPersister.merge(note);
      notePropertyIndexService.refreshForNote(note);

      assertThat(propertyRows(note), empty());
    }
  }

  @Test
  void deleting_note_cascades_index_rows() {
    User user = makeMe.aUser().please();
    Note note = makeMe.aNote().notebookOwnedBy(user).content("---\ntopic: a\n---\n").please();
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
