package com.odde.doughnut.services.book;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.entities.BookUserLastReadPosition;
import com.odde.doughnut.exceptions.ApiException;
import java.util.List;

public final class ReadingPositionLocatorResolver {

  private ReadingPositionLocatorResolver() {}

  public static ContentLocator resolve(BookUserLastReadPosition row, ObjectMapper objectMapper) {
    String json = row.getReadingPositionLocatorJson();
    if (json != null && !json.isBlank()) {
      try {
        return objectMapper.readValue(json, ContentLocator.class);
      } catch (JsonProcessingException e) {
        throw new ApiException(
            "stored reading position locator JSON is invalid",
            ApiError.ErrorType.BINDING_ERROR,
            "stored reading position locator JSON is invalid");
      }
    }
    ContentLocator fromLegacy = fromLegacyColumns(row);
    if (fromLegacy != null) {
      return fromLegacy;
    }
    throw new ApiException(
        "stored reading position has no locator JSON or legacy fields",
        ApiError.ErrorType.BINDING_ERROR,
        "stored reading position has no locator JSON or legacy fields");
  }

  private static ContentLocator fromLegacyColumns(BookUserLastReadPosition row) {
    String epub = trimToNull(row.getEpubLocator());
    if (epub != null) {
      return epubFromLegacyWireString(epub);
    }
    Integer pageIndex = row.getPageIndex();
    Integer normalizedY = row.getNormalizedY();
    if (pageIndex != null && normalizedY != null) {
      return new PdfLocator(
          pageIndex, List.of(0.0, normalizedY.doubleValue(), 100.0, 600.0), null, null);
    }
    return null;
  }

  private static EpubLocator epubFromLegacyWireString(String raw) {
    String t = raw.trim();
    int hash = t.indexOf('#');
    String hrefPart = hash < 0 ? t : t.substring(0, hash);
    String fragPart = hash < 0 ? null : t.substring(hash + 1);
    fragPart = trimToNull(fragPart);
    if (fragPart != null && fragPart.startsWith("#")) {
      fragPart = trimToNull(fragPart.substring(1));
    }
    hrefPart = trimToNull(hrefPart);
    if (hrefPart == null) {
      return null;
    }
    return new EpubLocator(hrefPart, fragPart);
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
