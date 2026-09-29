package com.odde.donut.services.book;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.services.book.MineruContentListLayoutBuilder.Bookmark;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

/**
 * Reads a PDF's bookmarks, positioned like MinerU {@code content_list} items: page index and a
 * 0–1000 y from the page top. Loading the file is also the attach readability check.
 */
final class PdfBookmarkReader {

  private PdfBookmarkReader() {}

  static List<Bookmark> read(byte[] bytes) {
    try (PDDocument doc = Loader.loadPDF(bytes)) {
      List<Bookmark> bookmarks = new ArrayList<>();
      PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();
      if (outline != null) {
        for (PDOutlineItem item : outline.children()) {
          bookmarks.add(bookmark(doc, item, 1));
        }
      }
      return bookmarks;
    } catch (IOException e) {
      throw new ApiException(
          "not a readable PDF", ApiError.ErrorType.BINDING_ERROR, "not a readable PDF");
    }
  }

  private static Bookmark bookmark(PDDocument doc, PDOutlineItem item, int level)
      throws IOException {
    PDPageXYZDestination dest = (PDPageXYZDestination) item.getDestination();
    int pageIdx = dest.retrievePageNumber();
    PDRectangle box = doc.getPage(pageIdx).getCropBox();
    double y = (box.getUpperRightY() - dest.getTop()) / box.getHeight() * 1000;
    return new Bookmark(level, item.getTitle(), pageIdx, y);
  }
}
