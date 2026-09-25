package com.odde.donut.controllers;

import static com.odde.donut.controllers.NotebookBooksControllerTestBase.attachRequest;
import static com.odde.donut.controllers.NotebookBooksControllerTestBase.epubAttachRequest;
import static com.odde.donut.controllers.NotebookBooksControllerTestBase.epubFile;
import static com.odde.donut.controllers.NotebookBooksControllerTestBase.node;
import static com.odde.donut.controllers.NotebookBooksControllerTestBase.pdfFile;
import static com.odde.donut.controllers.NotebookBooksControllerTestBase.readFixtureEpubValidMinimal;
import static com.odde.donut.controllers.NotebookBooksControllerTestBase.webRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.controllers.dto.AttachBookRequest;
import com.odde.donut.entities.Book;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.BookRepository;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookBooksAttachNotebookFileControllerTest
    extends NotebookGitWebContentControllerTestBase {
  @Autowired NotebookBooksController booksController;
  @Autowired BookRepository bookRepository;

  @Test
  void theBookAndItsFileAtTheNotebookRootAreAcceptedInOneCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    List<String> commitsBefore = acceptedHistory(notebook).commits();
    byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e};

    booksController.attachBook(notebook, physicsPrimer(), pdfFile(pdfBytes));

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(commitsBefore));
    assertThat(
        tipContent(after, "Physics Primer.pdf"), equalTo(lfsPointerStoredFor(notebook, pdfBytes)));
    assertThat(
        notebookAttachmentRepository
            .findByNotebook_IdAndFolderIsNullAndFilename(notebook.getId(), "Physics Primer.pdf")
            .isPresent(),
        equalTo(true));
    Book book = bookRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(book.getSourceFilePath(), equalTo("Physics Primer.pdf"));
    assertThat(book.getSourceFileRef(), nullValue());
    assertThat(booksController.getBookFile(webRequest(), notebook).getBody(), equalTo(pdfBytes));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void anEpubBooksFileIsNamedWithItsFormat() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    byte[] epubBytes = readFixtureEpubValidMinimal();

    booksController.attachBook(notebook, epubAttachRequest("Physics Primer"), epubFile(epubBytes));

    assertThat(
        tipContent(acceptedHistory(notebook), "Physics Primer.epub"),
        equalTo(lfsPointerStoredFor(notebook, epubBytes)));
  }

  @Test
  void aFileOverTenMebibytesIsAttachedAndReadBack() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    byte[] pdfBytes = new byte[11 * 1024 * 1024];

    booksController.attachBook(notebook, physicsPrimer(), pdfFile(pdfBytes));

    assertThat(booksController.getBookFile(webRequest(), notebook).getBody(), equalTo(pdfBytes));
  }

  private static AttachBookRequest physicsPrimer() {
    AttachBookRequest request = attachRequest(node("Chapter 1"));
    request.setBookName("Physics Primer");
    return request;
  }
}
