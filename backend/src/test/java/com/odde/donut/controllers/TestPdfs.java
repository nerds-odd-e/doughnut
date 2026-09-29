package com.odde.donut.controllers;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

/** Real PDF files built in code for book attach tests. */
final class TestPdfs {

  private TestPdfs() {}

  static byte[] onePagePdf(int blankContentBytes) {
    try (PDDocument doc = new PDDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PDPage page = new PDPage();
      page.setContents(
          new PDStream(
              doc, new ByteArrayInputStream(" ".repeat(blankContentBytes).getBytes(US_ASCII))));
      doc.addPage(page);
      doc.save(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** A top-level {@code /XYZ} bookmark at {@code yFromTop} (0–1000) on page {@code pageIdx}. */
  record PdfBookmark(String title, int pageIdx, int yFromTop) {}

  /** Pages are 1000×1000 points, so a bookmark's y matches MinerU's 0–1000 page coordinates. */
  static byte[] pdfWithBookmarks(int pageCount, PdfBookmark... bookmarks) {
    try (PDDocument doc = new PDDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      for (int i = 0; i < pageCount; i++) {
        doc.addPage(new PDPage(new PDRectangle(1000, 1000)));
      }
      PDDocumentOutline outline = new PDDocumentOutline();
      for (PdfBookmark bookmark : bookmarks) {
        PDPageXYZDestination dest = new PDPageXYZDestination();
        dest.setPage(doc.getPage(bookmark.pageIdx()));
        dest.setTop(1000 - bookmark.yFromTop());
        PDOutlineItem item = new PDOutlineItem();
        item.setTitle(bookmark.title());
        item.setDestination(dest);
        outline.addLast(item);
      }
      doc.getDocumentCatalog().setDocumentOutline(outline);
      doc.save(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
