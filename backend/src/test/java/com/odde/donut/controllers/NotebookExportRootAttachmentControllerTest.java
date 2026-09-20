package com.odde.donut.controllers;

import static com.odde.donut.services.notebookExport.PortableTreeEntry.ofText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;

/**
 * The notebook ZIP an owner downloads carries the root files they published, with their complete
 * filenames and their exact bytes, in the export's own canonical order: README, notes, attachments
 * by filename, then subfolders. The files get there through the ordinary publication boundary, not
 * through seeded projection rows.
 */
class NotebookExportRootAttachmentControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String NOTEBOOK_README = "---\ntype: Readme\n---\nnotebook readme";
  private static final String OVERVIEW_BODY = "---\ntype: Note\n---\nsee Diagram.png";
  private static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";
  private static final String REFERENCE_JSON = "{\"schema\": \"donut\"}\n";
  // Deliberately not valid UTF-8, so nothing may decode these bytes on the way out.
  private static final byte[] DIAGRAM_BYTES = {(byte) 0x89, (byte) 0xFF, (byte) 0xFE, 0x00};

  @Test
  void theExportedZipCarriesThePublishedRootFilesCompleteAndByteExact() throws Exception {
    Notebook notebook = publishRootFilesOverNotesAndFolders();

    Map<String, byte[]> entries = zipEntryBytes(controller.exportNotebook(notebook).getBody());

    assertThat(
        entries.keySet(),
        contains(
            "README.md",
            "Overview.md",
            "Diagram.png",
            "reference.json",
            "Biology/README.md",
            "Biology/Cells.md"));
    assertThat(entries.get("Diagram.png"), equalTo(DIAGRAM_BYTES));
    assertThat(entries.get("reference.json"), equalTo(utf8(REFERENCE_JSON)));
    assertThat(entries.get("Overview.md"), equalTo(utf8(OVERVIEW_BODY)));
  }

  /**
   * A notebook whose accepted tip already holds two root files beside a root note and a folder,
   * published through the ordinary publication controller.
   */
  private Notebook publishRootFilesOverNotesAndFolders() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology =
        makeMe.aFolder().notebook(notebook).name("Biology").readmeContent("readme").please();
    makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    makeMe.aNote("Overview").notebook(notebook).content(OVERVIEW_BODY).please();
    NotebookGitBinding markdownOnly = snapshotCurrentPortableTree(notebook);

    List<PortableTreeEntry> tip = new ArrayList<>(acceptedEntriesOf(markdownOnly));
    tip.add(ofText("README.md", NOTEBOOK_README));
    tip.add(new PortableTreeEntry("Diagram.png", DIAGRAM_BYTES));
    tip.add(ofText("reference.json", REFERENCE_JSON));
    controller.publishNotebookGitProposal(
        notebook.getId(),
        markdownOnly.getAcceptedGitObjectId(),
        proposalBundleBytes(markdownOnly, NotebookGitProposalFile.asProposal(tip)));

    return notebookRepository.findById(notebook.getId()).orElseThrow();
  }

  private List<PortableTreeEntry> acceptedEntriesOf(NotebookGitBinding binding) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      return GitBundleTestReader.readTreeEntries(
          repository,
          revWalk.parseCommit(GitBundleTestReader.fetchHead(repository, binding.getBundleBytes())));
    }
  }

  /** Raw bytes per entry: nothing here may decode a file on the way out. */
  private static Map<String, byte[]> zipEntryBytes(byte[] zipBytes) throws IOException {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        entries.put(entry.getName(), zis.readAllBytes());
      }
    }
    return entries;
  }

  private static byte[] utf8(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }
}
