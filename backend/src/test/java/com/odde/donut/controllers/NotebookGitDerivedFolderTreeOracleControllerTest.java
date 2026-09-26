package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.services.notebookTree.PortableTreeReadmeMarkdown;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;

/**
 * Every web folder change's accepted tree, derived by relocating accepted subtrees under the
 * folder's current prefix, must equal the tree a full assembly of the notebook's projection would
 * produce, without reading attachment content.
 */
class NotebookGitDerivedFolderTreeOracleControllerTest
    extends NotebookGitWebContentControllerTestBase {

  @Test
  void
      renamingAFolderRelocatesItsNotesAttachmentsAndSubfolderUnderTheNewPrefixAndMatchesTheFullAssembly()
          throws Throwable {
    Notebook notebook = createGitBackedNotebook();
    Folder photos = makeMe.aFolder().notebook(notebook).name("Photos").please();
    for (String title : List.of("Alps", "Beach", "City")) {
      makeMe.aNote(title).folder(photos).content(ACCEPTED_CONTENT).please();
    }
    Folder trips = makeMe.aFolder().parentFolder(photos).name("Trips").please();
    makeMe.aNote("Rome").folder(trips).content(ACCEPTED_CONTENT).please();
    storeFolderAttachmentAndSnapshot(notebook, photos, "alps.jpg", new byte[64]);
    storeFolderAttachmentAndSnapshot(notebook, photos, "beach.jpg", new byte[32]);
    storeFolderAttachmentAndSnapshot(notebook, trips, "rome.jpg", new byte[16]);
    Map<String, ObjectId> before = acceptedBlobIds(notebook);

    List<String> queries =
        queriesOf(() -> folderController.renameFolder(notebook, photos, renameTo("Pictures")));

    Map<String, ObjectId> relocated =
        before.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> entry.getKey().replaceFirst("^Photos/", "Pictures/"),
                    Map.Entry::getValue));
    assertThat(acceptedBlobIds(notebook), equalTo(relocated));
    assertThat(queries, not(hasItem(containsString("NotebookAttachment"))));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void creatingAFolderAddsItsMarkerAndMatchesTheFullAssembly() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder science = makeMe.aFolder().notebook(notebook).name("Science").please();
    snapshotCurrentPortableTree(notebook);
    FolderCreationRequest creation = new FolderCreationRequest();
    creation.setName("Biology");
    creation.setUnderFolderId(science.getId());

    folderController.createFolder(notebook, creation);

    assertThat(acceptedHistory(notebook).tipPaths(), contains("Science/Biology/.keep"));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void trashingAndRecoveringAFolderMoveItsEntriesWithTheirBlobsAndMatchTheFullAssembly()
      throws Throwable {
    Notebook notebook = createGitBackedNotebook();
    Folder research = makeMe.aFolder().notebook(notebook).name("Research").please();
    Folder biology = makeMe.aFolder().parentFolder(research).name("Biology").please();
    makeMe.aNote("Cells").folder(biology).content(ACCEPTED_CONTENT).please();
    storeFolderAttachmentAndSnapshot(notebook, biology, "cell.png", new byte[64]);
    Map<String, ObjectId> before = acceptedBlobIds(notebook);

    List<String> queries = queriesOf(() -> folderController.trashFolder(notebook, biology));

    Map<String, ObjectId> trashed = acceptedBlobIds(notebook);
    assertThat(
        acceptedHistory(notebook).tipPaths(),
        containsInAnyOrder(
            "Research/.keep",
            "_trash/Research/Biology/Cells.md",
            "_trash/Research/Biology/cell.png"));
    assertThat(
        trashed.get("_trash/Research/Biology/cell.png"),
        equalTo(before.get("Research/Biology/cell.png")));
    assertThat(queries, not(hasItem(containsString("NotebookAttachment"))));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);

    folderController.moveFolder(notebook, biology, folderMove(research.getId()));

    assertThat(
        acceptedHistory(notebook).tipPaths(),
        containsInAnyOrder(
            "Research/Biology/Cells.md", "Research/Biology/cell.png", "_trash/Research/.keep"));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void permanentlyDeletingAFolderDropsItsNotesAndAttachmentsWithItsPrefixAndMatchesTheFullAssembly()
      throws Throwable {
    Notebook notebook = createGitBackedNotebook();
    Folder trashedTopic = makeMe.aFolder().inTrashOf(notebook).name("Topic").please();
    Folder deeper = makeMe.aFolder().parentFolder(trashedTopic).name("Deeper").please();
    makeMe.aNote("Cells").folder(trashedTopic).content(ACCEPTED_CONTENT).please();
    makeMe.aNote("Nucleus").folder(deeper).content(ACCEPTED_CONTENT).please();
    storeFolderAttachmentAndSnapshot(notebook, trashedTopic, "a.pdf", new byte[64]);
    storeFolderAttachmentAndSnapshot(notebook, deeper, "b.pdf", new byte[32]);

    List<String> queries =
        queriesOf(() -> folderController.permanentlyDeleteFolder(notebook, trashedTopic));

    assertThat(acceptedHistory(notebook).tipPaths(), contains("_trash/.keep"));
    assertThat(
        "only the removal reads the subtree's attachment rows",
        queries.stream().filter(query -> query.contains("NotebookAttachment")).toList(),
        contains(containsString("folder.id IN :folderIds")));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void editingAFolderReadmeReplacesItsMarkerWithTheReadmeAndMatchesTheFullAssembly()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder docs = makeMe.aFolder().notebook(notebook).name("Docs").please();
    snapshotCurrentPortableTree(notebook);
    AcceptedHistory beforeEdit = acceptedHistory(notebook);
    NoteUpdateContentDTO dto = contentDto("Read these first");

    folderController.updateFolderReadmeContent(notebook, docs, dto);

    AcceptedHistory afterEdit = acceptedHistory(notebook);
    assertThat(afterEdit.parents(), equalTo(beforeEdit.commits()));
    assertThat(
        afterEdit.content(),
        contains(
            PortableTreeEntry.ofText(
                "Docs/README.md", PortableTreeReadmeMarkdown.assemble("Read these first"))));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);

    AcceptedHistory beforeClear = acceptedHistory(notebook);
    folderController.updateFolderReadmeContent(notebook, docs, contentDto(" "));

    AcceptedHistory afterClear = acceptedHistory(notebook);
    assertThat(afterClear.parents(), equalTo(beforeClear.commits()));
    assertThat(afterClear.tipPaths(), contains("Docs/.keep"));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void editingTheNotebookReadmePutsItAtTheRootWithoutAMarkerAndMatchesTheFullAssembly()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    snapshotCurrentPortableTree(notebook);
    AcceptedHistory beforeEdit = acceptedHistory(notebook);

    controller.updateNotebookReadmeContent(notebook, contentDto("Welcome"));

    AcceptedHistory afterEdit = acceptedHistory(notebook);
    assertThat(afterEdit.parents(), equalTo(beforeEdit.commits()));
    assertThat(
        afterEdit.content(),
        contains(
            PortableTreeEntry.ofText("README.md", PortableTreeReadmeMarkdown.assemble("Welcome"))));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);

    AcceptedHistory beforeClear = acceptedHistory(notebook);
    NoteUpdateContentDTO clear = new NoteUpdateContentDTO();
    clear.setContent(null);
    controller.updateNotebookReadmeContent(notebook, clear);

    AcceptedHistory afterClear = acceptedHistory(notebook);
    assertThat(afterClear.parents(), equalTo(beforeClear.commits()));
    assertThat(afterClear.tipPaths(), empty());
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }
}
