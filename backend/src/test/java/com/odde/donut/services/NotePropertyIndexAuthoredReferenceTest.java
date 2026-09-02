package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotePropertyIndex;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import com.odde.donut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotePropertyIndexAuthoredReferenceTest {

  @Autowired MakeMe makeMe;
  @Autowired NotePropertyIndexService notePropertyIndexService;
  @Autowired NotePropertyIndexRepository notePropertyIndexRepository;

  private List<NotePropertyIndex> propertyRows(Note note) {
    return notePropertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId());
  }

  @Test
  void stores_authored_reference_when_property_value_is_a_wiki_link() {
    User user = makeMe.aUser().please();
    Note existing = makeMe.aNote().title("Existing").notebookOwnedBy(user).please();
    Note note = makeMe.aNote().underSameNotebookAs(existing).please();
    makeMe.authorReferencingContent(note, "---\nexample of: \"[[Existing]]\"\n---\n");

    notePropertyIndexService.refreshForNote(note);

    NotePropertyIndex row = propertyRows(note).getFirst();
    assertThat(row.getPropertyKey(), equalTo("example of"));
    assertThat(row.getAuthoredNoteReference().getAuthoredLink(), equalTo("Existing"));
  }

  @Test
  void stores_null_reference_when_property_value_is_not_a_wiki_link() {
    User user = makeMe.aUser().please();
    Note note = makeMe.aNote().notebookOwnedBy(user).content("---\ntopic: physics\n---\n").please();

    notePropertyIndexService.refreshForNote(note);

    assertThat(propertyRows(note).getFirst().getAuthoredNoteReference(), nullValue());
  }

  @Test
  void indexes_non_empty_list_without_wiki_links_as_one_null_reference_row() {
    User user = makeMe.aUser().please();
    String markdown =
        "---\n" + "topic:\n" + "  - alpha\n" + "  - beta\n" + "  - gamma\n" + "---\n\nbody";
    Note note = makeMe.aNote().notebookOwnedBy(user).content(markdown).please();

    notePropertyIndexService.refreshForNote(note);

    List<NotePropertyIndex> rows = propertyRows(note);
    assertThat(rows, hasSize(1));
    assertThat(rows.getFirst().getPropertyKey(), equalTo("topic"));
    assertThat(rows.getFirst().getItemIndex(), equalTo(0));
    assertThat(rows.getFirst().getAuthoredNoteReference(), nullValue());
  }

  @Test
  void indexes_list_with_mixed_link_and_non_link_items_only_for_wiki_linked_items() {
    User user = makeMe.aUser().please();
    Note targetA = makeMe.aNote().title("A").notebookOwnedBy(user).please();
    String markdown =
        "---\n"
            + "example of:\n"
            + "  - \"[[A]]\"\n"
            + "  - plain text\n"
            + "  - \"[[C]]\"\n"
            + "---\n\nbody";
    Note note = makeMe.aNote().underSameNotebookAs(targetA).please();
    makeMe.authorReferencingContent(note, markdown);

    notePropertyIndexService.refreshForNote(note);

    List<NotePropertyIndex> rows = propertyRows(note);
    assertThat(rows, hasSize(2));
    assertThat(rows.get(0).getItemIndex(), equalTo(0));
    assertThat(rows.get(0).getAuthoredNoteReference().getAuthoredLink(), equalTo("A"));
    assertThat(rows.get(1).getItemIndex(), equalTo(2));
    assertThat(rows.get(1).getAuthoredNoteReference().getAuthoredLink(), equalTo("C"));
  }

  @Test
  void stores_authored_reference_even_when_wiki_link_does_not_resolve_to_an_existing_note() {
    User user = makeMe.aUser().please();
    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    makeMe.authorReferencingContent(note, "---\nexample of: \"[[Missing]]\"\n---\n");

    notePropertyIndexService.refreshForNote(note);

    assertThat(
        propertyRows(note).getFirst().getAuthoredNoteReference().getAuthoredLink(),
        equalTo("Missing"));
  }

  @Test
  void indexes_each_authored_wiki_link_in_a_list_regardless_of_whether_it_resolves() {
    User user = makeMe.aUser().please();
    String markdown =
        "---\n"
            + "example of:\n"
            + "  - \"[[Missing]]\"\n"
            + "  - \"[[AlsoMissing]]\"\n"
            + "---\n\nbody";
    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    makeMe.authorReferencingContent(note, markdown);

    notePropertyIndexService.refreshForNote(note);

    List<NotePropertyIndex> rows = propertyRows(note);
    assertThat(rows, hasSize(2));
    assertThat(rows.get(0).getItemIndex(), equalTo(0));
    assertThat(rows.get(0).getAuthoredNoteReference().getAuthoredLink(), equalTo("Missing"));
    assertThat(rows.get(1).getItemIndex(), equalTo(1));
    assertThat(rows.get(1).getAuthoredNoteReference().getAuthoredLink(), equalTo("AlsoMissing"));
  }
}
