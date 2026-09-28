package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.FolderTrailSegment;
import com.odde.donut.controllers.dto.NotebookAttachmentRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookAttachmentControllerFilePageTest extends ControllerTestBase {
  @Autowired NotebookAttachmentController controller;
  Notebook notebook;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
  }

  @Test
  void showsFilenameSizeAndFolderTrailThroughItsFolder() throws Exception {
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Folder data = makeMe.aFolder().parentFolder(physics).name("data").please();
    NotebookAttachment runJson =
        makeMe
            .anAttachment("run.json")
            .in(data)
            .content("{\"a\":1}".getBytes(StandardCharsets.UTF_8))
            .please();

    NotebookAttachmentRealm page = controller.getAttachmentPage(notebook, runJson);

    assertThat(page.attachment().filename(), equalTo("run.json"));
    assertThat(page.size(), equalTo(7L));
    assertThat(page.image(), equalTo(false));
    assertThat(
        page.sidebar().getAncestorFolders().stream().map(FolderTrailSegment::name).toList(),
        contains("physics", "data"));
    assertThat(page.sidebar().getNotebookRealm().notebook().getId(), equalTo(notebook.getId()));
  }

  @Test
  void rootFileHasNoFolderTrail() throws Exception {
    NotebookAttachment keep = attachmentAtRoot(".keep", "");

    assertThat(
        controller.getAttachmentPage(notebook, keep).sidebar().getAncestorFolders(), empty());
  }

  @Test
  void anImageFileIsMarkedAsAnImage() throws Exception {
    NotebookAttachment flow = attachmentAtRoot("flow.png", "png bytes");

    assertThat(controller.getAttachmentPage(notebook, flow).image(), equalTo(true));
  }

  @Test
  void listsSameFolderNotesWhoseImageIsThisFile() throws Exception {
    Folder docs = makeMe.aFolder().notebook(notebook).name("docs").please();
    NotebookAttachment diagram = attachmentIn(docs, "diagram.png");
    Note design = noteWithImage("Design", docs, "diagram.png");
    noteWithImage("Other", docs, "other.png");

    NotebookAttachmentRealm page = controller.getAttachmentPage(notebook, diagram);

    assertThat(page.references(), hasSize(1));
    assertThat(page.references().getFirst().getId(), equalTo(design.getId()));
  }

  @Test
  void aFileNoNoteNamesHasEmptyReferences() throws Exception {
    NotebookAttachment keep = attachmentAtRoot(".keep", "");

    assertThat(controller.getAttachmentPage(notebook, keep).references(), empty());
  }

  @Test
  void listsBothSameFolderNotesThatReferenceTheFile() throws Exception {
    Folder docs = makeMe.aFolder().notebook(notebook).name("docs").please();
    NotebookAttachment diagram = attachmentIn(docs, "diagram.png");
    Note design = noteWithImage("Design", docs, "diagram.png");
    Note review = noteWithImage("Review", docs, "diagram.png");

    NotebookAttachmentRealm page = controller.getAttachmentPage(notebook, diagram);

    assertThat(
        page.references().stream().map(r -> r.getId()).toList(),
        contains(design.getId(), review.getId()));
  }

  @Test
  void listsRootNoteWhoseFolderRelativeImagePathResolvesToThisFile() throws Exception {
    Folder docs = makeMe.aFolder().notebook(notebook).name("docs").please();
    NotebookAttachment diagram = attachmentIn(docs, "diagram.png");
    Note overview = noteWithImageAtRoot("Overview", "docs/diagram.png");

    NotebookAttachmentRealm page = controller.getAttachmentPage(notebook, diagram);

    assertThat(page.references(), hasSize(1));
    assertThat(page.references().getFirst().getId(), equalTo(overview.getId()));
  }

  @Test
  void sameFilenameInAnotherFolderIsNotConfused() throws Exception {
    Folder docs = makeMe.aFolder().notebook(notebook).name("docs").please();
    Folder archive = makeMe.aFolder().notebook(notebook).name("archive").please();
    NotebookAttachment docsDiagram = attachmentIn(docs, "diagram.png");
    NotebookAttachment archiveDiagram = attachmentIn(archive, "diagram.png");
    Note design = noteWithImage("Design", docs, "diagram.png");

    NotebookAttachmentRealm docsPage = controller.getAttachmentPage(notebook, docsDiagram);
    NotebookAttachmentRealm archivePage = controller.getAttachmentPage(notebook, archiveDiagram);

    assertThat(docsPage.references(), hasSize(1));
    assertThat(docsPage.references().getFirst().getId(), equalTo(design.getId()));
    assertThat(archivePage.references(), empty());
  }

  @Test
  void doesNotListTrashedNotes() throws Exception {
    Folder docs = makeMe.aFolder().inTrashOf(notebook).name("docs").please();
    NotebookAttachment diagram = attachmentIn(docs, "diagram.png");
    noteWithImage("Gone", docs, "diagram.png");

    assertThat(controller.getAttachmentPage(notebook, diagram).references(), empty());
  }

  private NotebookAttachment attachmentAtRoot(String filename, String content) {
    return makeMe
        .anAttachment(filename)
        .atRootOf(notebook)
        .content(content.getBytes(StandardCharsets.UTF_8))
        .please();
  }

  private NotebookAttachment attachmentIn(Folder folder, String filename) {
    return makeMe
        .anAttachment(filename)
        .in(folder)
        .content(filename.getBytes(StandardCharsets.UTF_8))
        .please();
  }

  private Note noteWithImage(String title, Folder folder, String image) {
    return makeMe
        .aNote(title)
        .folder(folder)
        .content("---\nimage: " + image + "\n---\nBody text")
        .please();
  }

  private Note noteWithImageAtRoot(String title, String image) {
    return makeMe
        .aNote(title)
        .notebook(notebook)
        .content("---\nimage: " + image + "\n---\nBody text")
        .please();
  }
}
