package com.odde.donut.services.book;

import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.entities.BookBlockTitleLimits;
import com.odde.donut.entities.BookContentBlock;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class BookBlockPdfContentLocators {

  private static final JsonMapper MAPPER = ObjectMapperConfig.handWrittenJsonMapper();

  private BookBlockPdfContentLocators() {}

  public static List<ContentLocator> pdfContentLocators(List<BookContentBlock> orderedBlocks) {
    if (orderedBlocks == null || orderedBlocks.isEmpty()) {
      return List.of();
    }
    List<ContentLocator> out = new ArrayList<>();
    PdfLocator anchor = pdfLocatorFromRaw(orderedBlocks.getFirst().getRawData(), null);
    if (anchor != null) {
      out.add(anchor);
    }
    for (int i = 1; i < orderedBlocks.size(); i++) {
      BookContentBlock cb = orderedBlocks.get(i);
      if (!BookBlockDirectContentPredicate.contributesDirectContent(cb)) {
        continue;
      }
      PdfLocator item = pdfLocatorFromRaw(cb.getRawData(), cb.getId());
      if (item != null) {
        out.add(item);
      }
    }
    return List.copyOf(out);
  }

  private static PdfLocator pdfLocatorFromRaw(String rawData, Integer contentBlockId) {
    if (rawData == null || rawData.isBlank()) {
      return null;
    }
    try {
      JsonNode n = MAPPER.readTree(rawData);
      JsonNode pageIdxNode = n.get("page_idx");
      if (pageIdxNode == null || pageIdxNode.isNull() || !pageIdxNode.isInt()) {
        return null;
      }
      int pageIndex = pageIdxNode.intValue();
      if (pageIndex < 0) {
        return null;
      }
      JsonNode bboxNode = n.get("bbox");
      if (bboxNode == null || !bboxNode.isArray() || bboxNode.size() != 4) {
        return null;
      }
      List<Double> bbox = new ArrayList<>(4);
      for (JsonNode c : bboxNode) {
        if (c == null || !c.isNumber()) {
          return null;
        }
        double v = c.doubleValue();
        if (!Double.isFinite(v)) {
          return null;
        }
        bbox.add(v);
      }
      double x0 = bbox.get(0);
      double y0 = bbox.get(1);
      double x1 = bbox.get(2);
      double y1 = bbox.get(3);
      if (x0 >= x1 || y0 >= y1) {
        return null;
      }
      String derivedTitle = contentBlockId != null ? derivedTitleFromRaw(n) : null;
      return new PdfLocator(pageIndex, List.copyOf(bbox), contentBlockId, derivedTitle);
    } catch (Exception e) {
      return null;
    }
  }

  private static String derivedTitleFromRaw(JsonNode n) {
    JsonNode text = n.get("text");
    if (text == null || !text.isString()) {
      return null;
    }
    String t = text.asString().strip();
    if (t.isEmpty()) {
      return null;
    }
    if (t.length() > BookBlockTitleLimits.STRUCTURAL_MAX_CHARS) {
      return t.substring(0, BookBlockTitleLimits.STRUCTURAL_MAX_CHARS);
    }
    return t;
  }
}
