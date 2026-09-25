package com.odde.donut.controllers;

import static com.odde.donut.controllers.NotebookBooksControllerTestBase.lastReadBody;
import static com.odde.donut.controllers.NotebookBooksControllerTestBase.rootBlocksSorted;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.configs.LegacyBookSourceFileMoveOnStartup;
import com.odde.donut.entities.Book;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.BookRepository;
import com.odde.donut.services.book.BookStorage;
import com.odde.donut.services.book.LegacyBookSourceFileMove;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LegacyBookSourceFileMoveControllerTest extends NotebookGitWebContentControllerTestBase {
  @Autowired LegacyBookSourceFileMove legacyBookSourceFileMove;
  @Autowired NotebookBooksController booksController;
  @Autowired BookRepository bookRepository;
  @Autowired BookStorage bookStorage;
  @Autowired ObjectMapper objectMapper;

  private static final byte[] PDF_BYTES = {0x25, 0x50, 0x44, 0x46};

  @Test
  void aLegacyBooksFileMovesToTheNotebookRootInOneDonutSystemCommitKeepingItsReadingData()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Book book = legacyBook(notebook);
    booksController.patchReadingPosition(notebook, lastReadBody(1, 200));
    booksController.putBlockReadingRecord(notebook, rootBlocksSorted(book).getFirst(), null);
    String readingBefore = readingData(notebook);
    List<String> commitsBefore = acceptedHistory(notebook).commits();

    legacyBookSourceFileMove.move(notebook.getId());

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(commitsBefore));
    assertThat(tipAuthorName(notebook), equalTo(NotebookGitCutoverService.SYSTEM_AUTHOR_NAME));
    assertThat(
        tipContent(after, "Physics Primer.pdf"), equalTo(lfsPointerStoredFor(notebook, PDF_BYTES)));
    assertThat(reloaded(book).getSourceFilePath(), equalTo("Physics Primer.pdf"));
    assertThat(readingData(notebook), equalTo(readingBefore));
    assertThat(bookStorage.get(book.getSourceFileRef()).orElseThrow(), equalTo(PDF_BYTES));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void startupMovesEveryNotebookLeavingAFailingOneUnchangedAndMovesNothingMoreWhenRerun()
      throws Exception {
    Notebook broken = createGitBackedNotebook("Broken");
    Book brokenBook = legacyBook(broken);
    NotebookGitBinding brokenBinding = binding(broken);
    deleteNativeObjectStoreRow(brokenBinding.getId(), brokenBinding.getAcceptedGitObjectId());
    Notebook healthy = createGitBackedNotebook("Healthy");
    Book healthyBook = legacyBook(healthy);
    LegacyBookSourceFileMoveOnStartup onStartup =
        new LegacyBookSourceFileMoveOnStartup(legacyBookSourceFileMove);

    onStartup.moveLegacyBookSourceFiles();

    assertThat(reloaded(brokenBook).getSourceFilePath(), nullValue());
    assertThat(reloaded(healthyBook).getSourceFilePath(), equalTo("Physics Primer.pdf"));
    List<String> healthyCommitsAfterFirstRun = acceptedHistory(healthy).commits();

    onStartup.moveLegacyBookSourceFiles();

    assertThat(acceptedHistory(healthy).commits(), equalTo(healthyCommitsAfterFirstRun));
    assertThat(reloaded(brokenBook).getSourceFilePath(), nullValue());
  }

  private Book legacyBook(Notebook notebook) {
    Book book =
        makeMe.aBook().notebook(notebook).bookName("Physics Primer").pdfBytes(PDF_BYTES).please();
    snapshotCurrentPortableTree(notebook);
    return book;
  }

  /** The Book as served (blocks and {@code updatedAt}), its reading records and position. */
  private String readingData(Notebook notebook) throws Exception {
    return objectMapper.writeValueAsString(
        List.of(
            booksController.getBook(notebook),
            booksController.getBookReadingRecords(notebook),
            booksController.getReadingPosition(notebook).getBody()));
  }

  private Book reloaded(Book book) {
    return bookRepository.findById(book.getId()).orElseThrow();
  }

  private String tipAuthorName(Notebook notebook) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      return revWalk
          .parseCommit(GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook)))
          .getAuthorIdent()
          .getName();
    }
  }
}
