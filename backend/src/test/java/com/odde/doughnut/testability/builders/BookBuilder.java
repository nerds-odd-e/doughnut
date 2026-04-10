package com.odde.doughnut.testability.builders;

import static com.odde.doughnut.services.book.BookReadingWireConstants.BOOK_FORMAT_PDF;

import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class BookBuilder extends EntityBuilder<Book> {

  private Notebook notebook;
  private byte[] pdfBytes = new byte[] {1};
  private String bookName = "Linear Algebra";
  private String rootBlockTitle = "X";

  public BookBuilder(MakeMe makeMe) {
    super(makeMe, new Book());
  }

  public BookBuilder notebook(Notebook notebook) {
    this.notebook = notebook;
    return this;
  }

  public BookBuilder bookName(String name) {
    this.bookName = name;
    return this;
  }

  public BookBuilder pdfBytes(byte[] bytes) {
    this.pdfBytes = bytes;
    return this;
  }

  public BookBuilder rootBlockTitle(String title) {
    this.rootBlockTitle = title;
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (notebook == null) {
      notebook =
          makeMe
              .aNotebook()
              .creatorAndOwner(makeMe.aUser().please(needPersist))
              .please(needPersist);
    }

    Timestamp now = makeMe.testabilitySettings.getCurrentUTCTimestamp();
    entity.setNotebook(notebook);
    entity.setBookName(bookName);
    entity.setFormat(BOOK_FORMAT_PDF);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    byte[] toStore = pdfBytes == null || pdfBytes.length == 0 ? new byte[] {1} : pdfBytes;
    entity.setSourceFileRef(makeMe.bookStorage.put(toStore));

    BookBlock block = new BookBlock();
    block.setStructuralTitle(rootBlockTitle);
    block.setStartAnchorValue("{\"p\":1}");
    block.setParent(null);
    block.setSiblingOrder(0);
    entity.addBlock(block);
  }
}
