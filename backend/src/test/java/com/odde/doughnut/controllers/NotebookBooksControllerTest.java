package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookRange;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.book.BookReadingWireConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksControllerTest extends ControllerTestBase {

  @Autowired NotebookBooksController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private static List<BookRange> rootRangesSorted(Book book) {
    return book.getRanges().stream()
        .filter(r -> r.getParentRangeId() == null)
        .sorted(Comparator.comparingLong(BookRange::getSiblingOrder))
        .toList();
  }

  private static List<BookRange> childrenOf(Book book, BookRange parent) {
    return book.getRanges().stream()
        .filter(r -> Objects.equals(r.getParentRangeId(), parent.getId()))
        .sorted(Comparator.comparingLong(BookRange::getSiblingOrder))
        .toList();
  }

  private static AttachBookAnchorRequest anchor(String value) {
    AttachBookAnchorRequest a = new AttachBookAnchorRequest();
    a.setAnchorFormat(BookReadingWireConstants.ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1);
    a.setValue(value);
    return a;
  }

  private static AttachBookLayoutNodeRequest node(
      String title, AttachBookLayoutNodeRequest... kids) {
    AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
    n.setTitle(title);
    n.setStartAnchor(anchor("{\"p\":1}"));
    n.setEndAnchor(anchor("{\"p\":2}"));
    if (kids != null && kids.length > 0) {
      n.setChildren(new ArrayList<>(List.of(kids)));
    }
    return n;
  }

  private static AttachBookRequest attachRequest(AttachBookLayoutNodeRequest... roots) {
    AttachBookLayoutRequest layout = new AttachBookLayoutRequest();
    layout.setRoots(new ArrayList<>(List.of(roots)));
    AttachBookRequest r = new AttachBookRequest();
    r.setBookName("Linear Algebra");
    r.setFormat(BookReadingWireConstants.BOOK_FORMAT_PDF);
    r.setLayout(layout);
    return r;
  }

  @Nested
  class AttachBook {
    @Test
    void persistsNestedOutlineAndReturnsBookWithRanges() throws UnexpectedNoAccessRightException {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      AttachBookLayoutNodeRequest ch1 = node("Section 1.1");
      AttachBookLayoutNodeRequest ch2 = node("Section 1.2");
      AttachBookLayoutNodeRequest root = node("Chapter 1", ch1, ch2);
      AttachBookRequest req = attachRequest(root);

      ResponseEntity<Book> res = controller.attachBook(nb, req);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.CREATED));
      assertThat(res.getBody(), notNullValue());
      Book created = res.getBody();
      assertThat(created.getId(), notNullValue());
      assertThat(created.getBookName(), equalTo("Linear Algebra"));
      assertThat(created.getRanges(), hasSize(3));

      BookRange outRoot = rootRangesSorted(created).getFirst();
      assertThat(outRoot.getStructuralTitle(), equalTo("Chapter 1"));
      assertThat(outRoot.getId(), notNullValue());
      List<BookRange> children = childrenOf(created, outRoot);
      assertThat(children, hasSize(2));
      assertThat(children.getFirst().getStructuralTitle(), equalTo("Section 1.1"));
      assertThat(children.get(1).getStructuralTitle(), equalTo("Section 1.2"));

      Book detail = controller.getBook(nb);
      assertThat(detail.getRanges(), hasSize(3));
      BookRange detailRoot = rootRangesSorted(detail).getFirst();
      assertThat(detailRoot.getId(), equalTo(outRoot.getId()));
      assertThat(childrenOf(detail, detailRoot), hasSize(2));
    }

    @Test
    void rejectsSecondAttachForSameNotebook() throws UnexpectedNoAccessRightException {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      controller.attachBook(nb, attachRequest(node("First")));
      assertThrows(
          ResponseStatusException.class,
          () -> controller.attachBook(nb, attachRequest(node("Second"))));
    }

    @Test
    void rejectsUnauthorizedNotebook() {
      User other = makeMe.aUser().please();
      Notebook otherNb = makeMe.aNotebook().creatorAndOwner(other).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.attachBook(otherNb, attachRequest(node("A"))));
    }

    @Test
    void rejectsNonPdfFormat() throws UnexpectedNoAccessRightException {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      AttachBookRequest req = attachRequest(node("A"));
      req.setFormat("epub");
      assertThrows(ResponseStatusException.class, () -> controller.attachBook(nb, req));
    }

    @Test
    void rejectsEmptyRoots() throws UnexpectedNoAccessRightException {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      AttachBookRequest req = attachRequest();
      req.getLayout().setRoots(new ArrayList<>());
      assertThrows(ResponseStatusException.class, () -> controller.attachBook(nb, req));
    }

    @Test
    void rejectsBadAnchorFormat() throws UnexpectedNoAccessRightException {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      AttachBookLayoutNodeRequest n = node("A");
      n.getStartAnchor().setAnchorFormat("unknown");
      assertThrows(
          ResponseStatusException.class, () -> controller.attachBook(nb, attachRequest(n)));
    }

    @Test
    void rejectsExcessiveDepth() throws UnexpectedNoAccessRightException {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      AttachBookLayoutNodeRequest deep = node("leaf");
      for (int i = 0; i < BookReadingWireConstants.MAX_LAYOUT_DEPTH; i++) {
        deep = node("d" + i, deep);
      }
      AttachBookLayoutNodeRequest root = deep;
      assertThrows(
          ResponseStatusException.class, () -> controller.attachBook(nb, attachRequest(root)));
    }
  }

  @Nested
  class GetBook {
    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb));
    }

    @Test
    void doesNotReturnAnotherNotebooksBook() throws UnexpectedNoAccessRightException {
      Notebook nb1 = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Notebook nb2 = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      controller.attachBook(nb1, attachRequest(node("X")));
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb2));
    }
  }
}
