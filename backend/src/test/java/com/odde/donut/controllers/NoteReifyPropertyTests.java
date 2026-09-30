package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.NoteReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NoteReifyPropertyTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @Autowired NoteRepository noteRepository;
  @Autowired NoteReferenceService noteReferenceService;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class WikiLinkProperty {
    Folder folder;
    Note source;
    NoteRealm result;
    Note relationshipNote;

    @BeforeEach
    void reify() throws UnexpectedNoAccessRightException {
      folder = makeMe.aFolder().notebookOwnedBy(currentUser.getUser()).name("F").please();
      source =
          makeMe
              .aNote("Src")
              .folder(folder)
              .content("---\nrelated: \"[[Other]]\"\nkind: example\n---\nbody")
              .please();
      makeMe.aNote("Other").folder(folder).please();
      noteReferenceService.refreshDerivedIndexesForNote(source);

      result = controller.reifyProperty(source, "related");
      relationshipNote = noteRepository.findById(result.getNote().getId()).orElseThrow();
    }

    @Test
    void createsTheRelationshipNoteInTheSourceNotebookAndFolder() {
      assertThat(relationshipNote.getNotebook().getId(), equalTo(source.getNotebook().getId()));
      assertThat(relationshipNote.getFolder().getId(), equalTo(folder.getId()));
    }

    @Test
    void titlesTheRelationshipNoteSourceRelationTarget() {
      assertThat(relationshipNote.getTitle(), equalTo("Src related Other"));
    }

    @Test
    void writesTheRelationshipFrontmatter() {
      assertThat(
          relationshipNote.getContent(),
          containsString(
              "type: Relationship\n"
                  + "relation: related\n"
                  + "source: \"[[Src]]\"\n"
                  + "target: \"[[Other]]\"\n"));
    }

    @Test
    void removesThePropertyFromTheSource() {
      assertThat(source.getContent(), not(containsString("related")));
    }
  }

  @Test
  void writesTheRelationInKebabForm() throws UnexpectedNoAccessRightException {
    Note source =
        makeMe
            .aNote("Moon")
            .notebookOwnedBy(currentUser.getUser())
            .content("---\na part of: \"[[Earth]]\"\n---\n")
            .please();
    makeMe.aNote("Earth").underSameNotebookAs(source).please();
    noteReferenceService.refreshDerivedIndexesForNote(source);

    NoteRealm result = controller.reifyProperty(source, "a part of");

    assertThat(result.getNote().getTitle(), equalTo("Moon a part of Earth"));
    assertThat(
        noteRepository.findById(result.getNote().getId()).orElseThrow().getContent(),
        containsString("relation: a-part-of\n"));
  }
}
