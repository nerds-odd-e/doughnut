package com.odde.doughnut.services.book;

import com.odde.doughnut.entities.BookContentBlock;
import java.util.List;

public enum BookFormat {
  PDF {
    @Override
    public List<ContentLocator> assembleContentLocators(List<BookContentBlock> contentBlocks) {
      return BookBlockPdfContentLocators.pdfContentLocators(contentBlocks);
    }
  },
  EPUB {
    @Override
    public List<ContentLocator> assembleContentLocators(List<BookContentBlock> contentBlocks) {
      return BookBlockEpubContentLocators.epubContentLocators(contentBlocks);
    }
  };

  public abstract List<ContentLocator> assembleContentLocators(
      List<BookContentBlock> contentBlocks);

  public static BookFormat fromString(String format) {
    if (BookReadingWireConstants.BOOK_FORMAT_EPUB.equals(format)) {
      return EPUB;
    }
    return PDF;
  }
}
