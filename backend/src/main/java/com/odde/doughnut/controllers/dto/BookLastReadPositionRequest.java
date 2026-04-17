package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.services.book.ContentLocator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookLastReadPositionRequest {

  @Schema(
      nullable = true,
      description =
          "Reading position as EpubLocator_Full or PdfLocator_Full. When set, overrides"
              + " pageIndex, normalizedY, and epubLocator.")
  private ContentLocator locator;

  @Schema(
      nullable = true,
      description =
          "0-based PDF page index in the viewer; required together with normalizedY for PDF books")
  private Integer pageIndex;

  @Schema(
      nullable = true,
      description =
          "Vertical position within the page in MinerU-normalized space (0-1000); required"
              + " together with pageIndex for PDF books")
  private Integer normalizedY;

  @Schema(
      nullable = true,
      description =
          "EPUB spine href locator (e.g. \"OEBPS/chapter2.xhtml#section-beta-two\") for EPUB"
              + " books; mutually exclusive with pageIndex/normalizedY")
  private String epubLocator;

  @Schema(
      nullable = true,
      description =
          "Selected book block id for reading UI; omit or null to leave the stored value unchanged")
  private Integer selectedBookBlockId;
}
