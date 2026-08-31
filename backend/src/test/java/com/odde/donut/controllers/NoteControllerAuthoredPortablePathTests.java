package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.AuthoredPortablePath;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NoteControllerAuthoredPortablePathTests extends ControllerTestBase {
  @Autowired NoteController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void shouldAuthorDisplayNameShorthandWhenDisplayNameIsUnique()
      throws UnexpectedNoAccessRightException {
    Folder pantry =
        makeMe.aFolder().notebookOwnedBy(currentUser.getUser()).name("WikiDup Pantry").please();
    Note destination = makeMe.aNote().title("WikiDup Shared").folder(pantry).please();
    Note source = makeMe.aNote().notebook(pantry.getNotebook()).please();

    AuthoredPortablePath authored =
        controller.authoredPortablePath(source, destination, "WikiDup Shared");

    assertThat(authored.portablePath(), equalTo("WikiDup Shared"));
  }

  @Test
  void shouldPreservePropertySelectorOnAuthoredPortablePath()
      throws UnexpectedNoAccessRightException {
    Folder solar = makeMe.aFolder().notebookOwnedBy(currentUser.getUser()).name("Solar").please();
    Note destination = makeMe.aNote().title("Moon").folder(solar).please();
    Note source = makeMe.aNote().notebook(solar.getNotebook()).please();

    AuthoredPortablePath authored =
        controller.authoredPortablePath(source, destination, "Moon#prop:topic");

    assertThat(authored.portablePath(), equalTo("Moon#prop:topic"));
  }

  @Test
  void shouldAuthorFullNormalizedFolderPathWhenDisplayNameIsAmbiguous()
      throws UnexpectedNoAccessRightException {
    Folder pantry =
        makeMe.aFolder().notebookOwnedBy(currentUser.getUser()).name("WikiDup Pantry").please();
    Note destination = makeMe.aNote().title("WikiDup Shared").folder(pantry).please();
    Folder recipes =
        makeMe.aFolder().notebook(pantry.getNotebook()).name("WikiDup Recipes").please();
    makeMe.aNote().title("WikiDup Shared").folder(recipes).please();
    Note source = makeMe.aNote().notebook(pantry.getNotebook()).please();

    AuthoredPortablePath authored =
        controller.authoredPortablePath(source, destination, "WikiDup Shared");

    assertThat(authored.portablePath(), equalTo("WikiDup Pantry/WikiDup Shared"));
  }

  @Test
  void shouldAuthorFullNormalizedFolderPathWhenDisplayNameCollidesWithAlias()
      throws UnexpectedNoAccessRightException {
    Folder pantry =
        makeMe.aFolder().notebookOwnedBy(currentUser.getUser()).name("WikiDup Pantry").please();
    Note destination = makeMe.aNote().title("color").folder(pantry).please();
    makeMe.aNote().title("colour").notebook(pantry.getNotebook()).aliases("color").please();
    Note source = makeMe.aNote().notebook(pantry.getNotebook()).please();

    AuthoredPortablePath authored = controller.authoredPortablePath(source, destination, "color");

    assertThat(authored.portablePath(), equalTo("WikiDup Pantry/color"));
  }

  @Test
  void shouldAuthorExactRootPathWhenRootDisplayNameIsAmbiguousAsShorthand()
      throws UnexpectedNoAccessRightException {
    Note destination =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("WikiDup Shared").please();
    Folder pantry =
        makeMe.aFolder().notebook(destination.getNotebook()).name("WikiDup Pantry").please();
    makeMe.aNote().title("WikiDup Shared").folder(pantry).please();
    Note source = makeMe.aNote().notebook(destination.getNotebook()).please();

    AuthoredPortablePath authored =
        controller.authoredPortablePath(source, destination, "WikiDup Shared");

    assertThat(authored.portablePath(), equalTo("/WikiDup Shared"));
  }

  @Test
  void shouldQualifyPortablePathWithNotebookNameWhenDestinationNotebookDiffersFromSource()
      throws UnexpectedNoAccessRightException {
    Notebook solarNotebook =
        makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).name("Solar").please();
    Note destination = makeMe.aNote().notebook(solarNotebook).title("Moon").please();
    Note source = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

    AuthoredPortablePath authored = controller.authoredPortablePath(source, destination, "Moon");

    assertThat(authored.portablePath(), equalTo("Solar:Moon"));
  }
}
