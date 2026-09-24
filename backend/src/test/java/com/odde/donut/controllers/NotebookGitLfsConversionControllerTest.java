package com.odde.donut.controllers;

import static com.odde.donut.services.notebookAttachment.VerifiedNotebookAttachmentBytes.sha256Hex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.odde.donut.configs.NotebookGitLfsConversionOnStartup;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.services.notebookGit.NotebookGitLfsConversionService;
import com.odde.donut.services.notebookGit.NotebookGitLfsPointer;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Converting a notebook still on legacy raw attachment storage moves it to LFS with one forward
 * Donut System commit; a notebook already on LFS is left as it is. When the startup trigger
 * converts every raw notebook, one notebook's failure leaves it raw without stopping the others.
 */
class NotebookGitLfsConversionControllerTest extends NotebookGitWebContentControllerTestBase {

  private static final String NOTE_CONTENT = "---\ntype: Note\n---\nkept as it is";

  private static final byte[] DIAGRAM_V1 = {(byte) 0x89, 'P', 'N', 'G', 1};
  private static final byte[] DIAGRAM_V2 = {(byte) 0x89, 'P', 'N', 'G', 2, (byte) 0xFF};
  private static final byte[] PAPER = {'%', 'P', 'D', 'F', (byte) 0xFE};

  @Autowired NotebookGitLfsConversionService conversionService;
  @Autowired NotebookAttachmentController attachmentController;
  @Autowired NotebookAttachmentContent attachmentContent;

  @Test
  void currentFilesBecomePointersWhoseWebDownloadsKeepTheirBytes() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    publish(
        notebook,
        List.of(
            new NotebookGitProposalFile("physics/diagram.png", DIAGRAM_V1),
            new NotebookGitProposalFile("refs/paper.pdf", PAPER),
            new NotebookGitProposalFile("refs/empty.dat", new byte[0])));
    publish(
        notebook,
        List.of(
            new NotebookGitProposalFile("physics/diagram.png", DIAGRAM_V2),
            new NotebookGitProposalFile("refs/paper.pdf", PAPER),
            new NotebookGitProposalFile("refs/empty.dat", new byte[0])));
    String firstPublication = acceptedHistory(notebook).commits().get(1);

    conversionService.convert(notebook.getId(), Instant.now());

    AcceptedHistory converted = acceptedHistory(notebook);
    assertThat(
        converted.tipContent(),
        equalTo(
            List.of(
                NotebookGitAttributes.initialEntry(),
                new PortableTreeEntry("physics/diagram.png", pointerOf(DIAGRAM_V2)),
                new PortableTreeEntry("refs/empty.dat", new byte[0]),
                new PortableTreeEntry("refs/paper.pdf", pointerOf(PAPER)))));
    Map<String, NotebookAttachment> rows = new HashMap<>();
    notebookAttachmentRepository
        .findByNotebook_Id(notebook.getId())
        .forEach(row -> rows.put(row.getFilename(), row));
    assertThat(rows.get("diagram.png").getAcceptedGitContent(), equalTo(pointerOf(DIAGRAM_V2)));
    assertThat(rows.get("paper.pdf").getAcceptedGitContent(), equalTo(pointerOf(PAPER)));
    assertThat(rows.get("empty.dat").getAcceptedGitContent(), equalTo(new byte[0]));
    assertThat(download(notebook, rows.get("diagram.png")), equalTo(DIAGRAM_V2));
    assertThat(download(notebook, rows.get("paper.pdf")), equalTo(PAPER));
    assertThat(download(notebook, rows.get("empty.dat")), equalTo(new byte[0]));
    assertThat(
        attachmentContent.get(notebook.getId(), sha256Hex(DIAGRAM_V2)).orElseThrow(),
        equalTo(DIAGRAM_V2));
    assertThat(
        attachmentContent.get(notebook.getId(), sha256Hex(PAPER)).orElseThrow(), equalTo(PAPER));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      assertThat(
          GitBundleTestReader.readTreeEntries(
              repository, revWalk.parseCommit(ObjectId.fromString(firstPublication))),
          hasItem(new PortableTreeEntry("physics/diagram.png", DIAGRAM_V1)));
    }
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void aRawNotebookWithoutFilesBecomesLfsInOneForwardCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("note").content(NOTE_CONTENT).please();
    String previousHead = snapshotCurrentPortableTree(notebook).getAcceptedGitObjectId();
    List<PortableTreeEntry> expectedEntries =
        new ArrayList<>(acceptedHistory(notebook).tipContent());
    expectedEntries.add(0, NotebookGitAttributes.initialEntry());

    conversionService.convert(notebook.getId(), Instant.now());

    assertThat(representationOf(notebook), is(NotebookGitAttachmentRepresentation.LFS));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      ObjectId head = GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      RevCommit conversion = revWalk.parseCommit(head);
      assertThat(conversion.getParentCount(), equalTo(1));
      assertThat(conversion.getParent(0).name(), equalTo(previousHead));
      assertThat(
          conversion.getAuthorIdent().getName(),
          equalTo(NotebookGitCutoverService.SYSTEM_AUTHOR_NAME));
      assertThat(
          conversion.getAuthorIdent().getEmailAddress(),
          equalTo(NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL));
      assertThat(conversion.getFullMessage(), equalTo("Store notebook files with Git LFS"));
      assertThat(
          GitBundleTestReader.readTreeEntriesWithMetadata(repository, conversion),
          equalTo(expectedEntries));
    }
  }

  @Test
  void convertingAgainLeavesTheHeadUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    conversionService.convert(notebook.getId(), Instant.now());
    AcceptedHistory converted = acceptedHistory(notebook);

    conversionService.convert(notebook.getId(), Instant.now());

    assertThat(acceptedHistory(notebook), equalTo(converted));
  }

  @Test
  void anLfsNotebookIsLeftAsItIs() throws Exception {
    Notebook notebook = createProductLfsNotebook();
    AcceptedHistory before = acceptedHistory(notebook);

    conversionService.convert(notebook.getId(), Instant.now());

    assertThat(acceptedHistory(notebook), equalTo(before));
  }

  @Test
  void aNotebookThatFailsToConvertStaysRawWhileTheOthersConvert() throws Exception {
    Notebook broken = createGitBackedNotebook("Broken");
    NotebookGitBinding brokenBinding = reloadCommittedBinding(broken.getId());
    deleteNativeObjectStoreRow(brokenBinding.getId(), brokenBinding.getAcceptedGitObjectId());
    Notebook healthy = createGitBackedNotebook("Healthy");

    new NotebookGitLfsConversionOnStartup(notebookGitBindingRepository, conversionService)
        .convertRawNotebooks();

    assertThat(representationOf(broken), is(NotebookGitAttachmentRepresentation.RAW));
    assertThat(representationOf(healthy), is(NotebookGitAttachmentRepresentation.LFS));
  }

  private void publish(Notebook notebook, List<NotebookGitProposalFile> files) throws Exception {
    NotebookGitBinding accepted = reloadCommittedBinding(notebook.getId());
    controller.publishNotebookGitProposal(
        notebook.getId(), accepted.getAcceptedGitObjectId(), proposalBundleBytes(accepted, files));
  }

  private byte[] download(Notebook notebook, NotebookAttachment attachment) throws Exception {
    return attachmentController.downloadAttachment(notebook, attachment).getBody();
  }

  private static byte[] pointerOf(byte[] bytes) {
    return NotebookGitLfsPointer.format(sha256Hex(bytes), bytes.length);
  }

  private NotebookGitAttachmentRepresentation representationOf(Notebook notebook) {
    return reloadCommittedBinding(notebook.getId()).getAttachmentRepresentation();
  }
}
