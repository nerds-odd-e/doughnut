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
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;

/**
 * Reads a PDF's bookmarks, positioned like MinerU {@code content_list} items: page index and a
 * 0–1000 y from the page top, in outline order with nesting as levels. Loading the file is also the
 * attach readability check.
 */
final class PdfBookmarkReader {

  private PdfBookmarkReader() {}

  static List<Bookmark> read(byte[] bytes) {
    try (PDDocument doc = Loader.loadPDF(bytes)) {
      List<Bookmark> bookmarks = new ArrayList<>();
      PDOutlineNode outline = doc.getDocumentCatalog().getDocumentOutline();
      if (outline != null) {
        addBookmarks(doc, outline, 1, bookmarks);
      }
      return bookmarks;
    } catch (IOException e) {
      throw new ApiException(
          "not a readable PDF", ApiError.ErrorType.BINDING_ERROR, "not a readable PDF");
    }
  }

  private static void addBookmarks(
      PDDocument doc, PDOutlineNode parent, int level, List<Bookmark> bookmarks)
      throws IOException {
    for (PDOutlineItem item : parent.children()) {
      bookmarks.add(bookmark(doc, item, level));
      addBookmarks(doc, item, level + 1, bookmarks);
    }
  }

  private static Bookmark bookmark(PDDocument doc, PDOutlineItem item, int level)
      throws IOException {
    PDPageDestination dest = pageDestination(doc, item);
    int pageIdx = dest.retrievePageNumber();
    return new Bookmark(level, item.getTitle(), pageIdx, y(doc, pageIdx, dest));
  }

  /** The page a bookmark points to, directly, by name, or through a GoTo action. */
  private static PDPageDestination pageDestination(PDDocument doc, PDOutlineItem item)
      throws IOException {
    PDDestination dest = item.getDestination();
    if (dest == null) {
      dest = ((PDActionGoTo) item.getAction()).getDestination();
    }
    if (dest instanceof PDNamedDestination named) {
      return doc.getDocumentCatalog().findNamedDestinationPage(named);
    }
    return (PDPageDestination) dest;
  }

  /** The top of an {@code /XYZ} destination; the page top for any destination without one. */
  private static double y(PDDocument doc, int pageIdx, PDPageDestination dest) {
    if (!(dest instanceof PDPageXYZDestination xyz) || xyz.getTop() == -1) {
      return 0;
    }
    PDRectangle box = doc.getPage(pageIdx).getCropBox();
    return (box.getUpperRightY() - xyz.getTop()) / box.getHeight() * 1000;
  }
}
