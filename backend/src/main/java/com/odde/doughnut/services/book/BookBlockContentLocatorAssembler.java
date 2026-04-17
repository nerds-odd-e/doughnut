package com.odde.doughnut.services.book;

import com.odde.doughnut.entities.BookContentBlock;
import java.util.List;

public final class BookBlockContentLocatorAssembler {

  private BookBlockContentLocatorAssembler() {}

  public static List<ContentLocator> assemble(String format, List<BookContentBlock> contentBlocks) {
    if (BookReadingWireConstants.BOOK_FORMAT_EPUB.equals(format)) {
      return BookBlockEpubContentLocators.epubContentLocators(contentBlocks);
    }
    return BookBlockPdfContentLocators.pdfContentLocators(contentBlocks);
  }
}
