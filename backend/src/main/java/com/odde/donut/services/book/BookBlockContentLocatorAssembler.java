package com.odde.donut.services.book;

import com.odde.donut.entities.BookContentBlock;
import java.util.List;

public final class BookBlockContentLocatorAssembler {

  private BookBlockContentLocatorAssembler() {}

  public static List<ContentLocator> assemble(
      BookFormat format, List<BookContentBlock> contentBlocks) {
    return format.assembleContentLocators(contentBlocks);
  }

  public static List<ContentLocator> assemble(String format, List<BookContentBlock> contentBlocks) {
    return assemble(BookFormat.fromString(format), contentBlocks);
  }
}
