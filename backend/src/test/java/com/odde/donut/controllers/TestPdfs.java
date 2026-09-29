package com.odde.donut.controllers;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.pdfbox.pdmodel.PDDestinationNameTreeNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;

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

  /**
   * A bookmark to page {@code pageIdx}: an {@code /XYZ} destination at {@code yFromTop} (0–1000),
   * or {@code /Fit} when {@code yFromTop} is null. With a {@code destinationName}, it goes there
   * through a GoTo action to that named destination, as LaTeX PDFs do.
   */
  record PdfBookmark(
      String title,
      int pageIdx,
      Integer yFromTop,
      String destinationName,
      List<PdfBookmark> children) {

    PdfBookmark(String title, int pageIdx, int yFromTop, PdfBookmark... children) {
      this(title, pageIdx, yFromTop, null, List.of(children));
    }

    static PdfBookmark fit(String title, int pageIdx) {
      return new PdfBookmark(title, pageIdx, null, null, List.of());
    }

    PdfBookmark named(String name) {
      return new PdfBookmark(title, pageIdx, yFromTop, name, children);
    }
  }

  /** Pages are 1000×1000 points, so a bookmark's y matches MinerU's 0–1000 page coordinates. */
  static byte[] pdfWithBookmarks(int pageCount, PdfBookmark... bookmarks) {
    try (PDDocument doc = new PDDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      for (int i = 0; i < pageCount; i++) {
        doc.addPage(new PDPage(new PDRectangle(1000, 1000)));
      }
      PDDocumentOutline outline = new PDDocumentOutline();
      Map<String, PDPageDestination> namedDests = new TreeMap<>();
      addBookmarks(doc, outline, List.of(bookmarks), namedDests);
      doc.getDocumentCatalog().setDocumentOutline(outline);
      if (!namedDests.isEmpty()) {
        PDDestinationNameTreeNode dests = new PDDestinationNameTreeNode();
        dests.setNames(namedDests);
        PDDocumentNameDictionary names = new PDDocumentNameDictionary(doc.getDocumentCatalog());
        names.setDests(dests);
        doc.getDocumentCatalog().setNames(names);
      }
      doc.save(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void addBookmarks(
      PDDocument doc,
      PDOutlineNode parent,
      List<PdfBookmark> bookmarks,
      Map<String, PDPageDestination> namedDests) {
    for (PdfBookmark bookmark : bookmarks) {
      PDPageDestination dest;
      if (bookmark.yFromTop() == null) {
        dest = new PDPageFitDestination();
      } else {
        PDPageXYZDestination xyz = new PDPageXYZDestination();
        xyz.setTop(1000 - bookmark.yFromTop());
        dest = xyz;
      }
      dest.setPage(doc.getPage(bookmark.pageIdx()));
      PDOutlineItem item = new PDOutlineItem();
      item.setTitle(bookmark.title());
      if (bookmark.destinationName() == null) {
        item.setDestination(dest);
      } else {
        namedDests.put(bookmark.destinationName(), dest);
        PDActionGoTo goTo = new PDActionGoTo();
        goTo.setDestination(new PDNamedDestination(bookmark.destinationName()));
        item.setAction(goTo);
      }
      parent.addLast(item);
      addBookmarks(doc, item, bookmark.children(), namedDests);
    }
  }
}
