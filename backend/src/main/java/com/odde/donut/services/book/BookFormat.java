package com.odde.donut.services.book;

import static com.odde.donut.services.book.BookReadingWireConstants.BOOK_FORMAT_EPUB;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.AttachBookRequest;
import com.odde.donut.entities.Book;
import com.odde.donut.entities.BookContentBlock;
import com.odde.donut.entities.BookUserLastReadPosition;
import com.odde.donut.exceptions.ApiException;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public enum BookFormat {
  PDF {
    @Override
    public List<ContentLocator> assembleContentLocators(List<BookContentBlock> contentBlocks) {
      return BookBlockPdfContentLocators.pdfContentLocators(contentBlocks);
    }

    @Override
    public void validateAttachRequest(AttachBookRequest request) {
      AttachBookLayoutValidator.validatePdfAttachRequest(request);
    }

    @Override
    public Book persistNewBook(BookService.PersistContext ctx) {
      return AttachBookPersistence.persistNewPdfBook(ctx);
    }

    @Override
    public MediaType bookFileMediaType() {
      return MediaType.APPLICATION_PDF;
    }

    @Override
    public String bookFileExtension() {
      return ".pdf";
    }

    @Override
    public void writeReadingPositionLocator(
        BookUserLastReadPosition row, ContentLocator locator, ObjectMapper objectMapper) {
      if (!(locator instanceof PdfLocator pdf)) {
        throw new ApiException(
            "PdfLocator_Full required for PDF reading-position locator",
            ApiError.ErrorType.BINDING_ERROR,
            "PdfLocator_Full required for PDF reading-position locator");
      }
      List<Double> bbox = pdf.bbox();
      if (bbox == null || bbox.size() != 4) {
        throw new ApiException(
            "PdfLocator_Full bbox must have four numbers",
            ApiError.ErrorType.BINDING_ERROR,
            "PdfLocator_Full bbox must have four numbers");
      }
      persistReadingPositionLocatorJson(row, objectMapper, locator);
    }
  },
  EPUB {
    @Override
    public List<ContentLocator> assembleContentLocators(List<BookContentBlock> contentBlocks) {
      return BookBlockEpubContentLocators.epubContentLocators(contentBlocks);
    }

    @Override
    public void validateAttachRequest(AttachBookRequest request) {
      AttachBookLayoutValidator.validateEpubAttachRequest(request);
    }

    @Override
    public Book persistNewBook(BookService.PersistContext ctx) {
      return AttachBookPersistence.persistNewEpubBook(ctx);
    }

    @Override
    public MediaType bookFileMediaType() {
      return MediaType.parseMediaType("application/epub+zip");
    }

    @Override
    public String bookFileExtension() {
      return ".epub";
    }

    @Override
    public void writeReadingPositionLocator(
        BookUserLastReadPosition row, ContentLocator locator, ObjectMapper objectMapper) {
      if (!(locator instanceof EpubLocator epub)) {
        throw new ApiException(
            "EpubLocator_Full required for EPUB reading-position locator",
            ApiError.ErrorType.BINDING_ERROR,
            "EpubLocator_Full required for EPUB reading-position locator");
      }
      String href = trimToNull(epub.href());
      if (href == null) {
        throw new ApiException(
            "EpubLocator_Full href is required",
            ApiError.ErrorType.BINDING_ERROR,
            "EpubLocator_Full href is required");
      }
      String frag = epub.fragment();
      if (frag != null && frag.startsWith("#")) {
        frag = frag.substring(1);
      }
      frag = trimToNull(frag);
      persistReadingPositionLocatorJson(row, objectMapper, new EpubLocator(href, frag));
    }
  };

  public abstract List<ContentLocator> assembleContentLocators(
      List<BookContentBlock> contentBlocks);

  public abstract void validateAttachRequest(AttachBookRequest request);

  public abstract Book persistNewBook(BookService.PersistContext ctx);

  public abstract MediaType bookFileMediaType();

  public abstract String bookFileExtension();

  public abstract void writeReadingPositionLocator(
      BookUserLastReadPosition row, ContentLocator locator, ObjectMapper objectMapper);

  public static BookFormat forLocator(ContentLocator locator) {
    return switch (locator) {
      case EpubLocator e -> EPUB;
      case PdfLocator p -> PDF;
    };
  }

  public final ResponseEntity<Resource> streamFile(
      byte[] bytes, String baseName, String etag, CacheControl cacheControl) {
    return ResponseEntity.ok()
        .eTag(etag)
        .cacheControl(cacheControl)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "inline; filename=\"" + baseName + bookFileExtension() + "\"")
        .contentType(bookFileMediaType())
        .body(new ByteArrayResource(bytes));
  }

  public static BookFormat fromString(String format) {
    if (BOOK_FORMAT_EPUB.equals(format)) {
      return EPUB;
    }
    return PDF;
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static void persistReadingPositionLocatorJson(
      BookUserLastReadPosition row, ObjectMapper objectMapper, ContentLocator locator) {
    try {
      row.setReadingPositionLocatorJson(objectMapper.writeValueAsString(locator));
    } catch (JsonProcessingException e) {
      throw new ApiException(
          "failed to serialize reading position locator",
          ApiError.ErrorType.BINDING_ERROR,
          "failed to serialize reading position locator");
    }
  }
}
