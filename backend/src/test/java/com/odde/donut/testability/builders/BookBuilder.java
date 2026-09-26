package com.odde.donut.testability.builders;

import static com.odde.donut.services.book.BookReadingWireConstants.BOOK_FORMAT_PDF;

import com.odde.donut.entities.Book;
import com.odde.donut.entities.BookBlock;
import com.odde.donut.entities.Notebook;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;
import java.sql.Timestamp;

public class BookBuilder extends EntityBuilder<Book> {

  private Notebook notebook;
  private byte[] fileBytes = new byte[] {1};
  private String bookName = "Linear Algebra";
  private String format = BOOK_FORMAT_PDF;
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

  public BookBuilder format(String format) {
    this.format = format;
    return this;
  }

  /** The bytes of the Book's source file, placed at the notebook root. */
  public BookBuilder fileBytes(byte[] bytes) {
    this.fileBytes = bytes;
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
    entity.setFormat(format);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    String sourceFilePath = bookName + "." + format;
    makeMe.anAttachment(sourceFilePath).atRootOf(notebook).content(fileBytes).please(needPersist);
    entity.setSourceFilePath(sourceFilePath);

    BookBlock block = new BookBlock();
    block.setStructuralTitle(rootBlockTitle);
    block.setLayoutSequence(0);
    block.setDepth(0);
    entity.addBlock(block);
  }
}
