package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.BookUserLastReadPosition;
import com.odde.doughnut.services.book.ContentLocator;
import com.odde.doughnut.services.book.EpubLocator;
import com.odde.doughnut.services.book.PdfLocator;
import com.odde.doughnut.services.book.ReadingPositionLocatorResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "BookUserLastReadPosition")
public record BookUserLastReadPositionResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ContentLocator locator,
    @Schema(nullable = true) Integer selectedBookBlockId,
    @Schema(
            nullable = true,
            description = "Derived from locator for API backward compatibility (PDF: page index)")
        Integer pageIndex,
    @Schema(
            nullable = true,
            description = "Derived from locator for API backward compatibility (PDF: normalized Y)")
        Integer normalizedY,
    @Schema(
            nullable = true,
            description =
                "Derived from locator for API backward compatibility (EPUB spine locator string)")
        String epubLocator) {

  public static BookUserLastReadPositionResponse from(
      BookUserLastReadPosition row, ObjectMapper objectMapper) {
    ContentLocator locator = ReadingPositionLocatorResolver.resolve(row, objectMapper);
    Integer pageIndex = null;
    Integer normalizedY = null;
    String epubLocator = null;
    switch (locator) {
      case PdfLocator pdf -> {
        pageIndex = pdf.pageIndex();
        List<Double> bbox = pdf.bbox();
        if (bbox != null && bbox.size() >= 2) {
          normalizedY = (int) Math.round(Math.max(0, Math.min(1000, bbox.get(1))));
        }
      }
      case EpubLocator epub -> {
        String href = epub.href();
        String frag = epub.fragment();
        if (frag == null || frag.isEmpty()) {
          epubLocator = href;
        } else {
          epubLocator = href + "#" + frag;
        }
      }
    }
    return new BookUserLastReadPositionResponse(
        row.getId(), locator, row.getSelectedBookBlockId(), pageIndex, normalizedY, epubLocator);
  }
}
