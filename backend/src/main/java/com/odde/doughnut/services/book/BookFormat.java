package com.odde.doughnut.services.book;

import static com.odde.doughnut.services.book.BookReadingWireConstants.MAX_CONTENT_LIST_ITEMS;
import static com.odde.doughnut.services.book.BookReadingWireConstants.MAX_LAYOUT_DEPTH;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.doughnut.controllers.dto.AttachBookLayoutRequest;
import com.odde.doughnut.controllers.dto.AttachBookRequest;
import com.odde.doughnut.entities.BookBlockTitleLimits;
import com.odde.doughnut.entities.BookContentBlock;
import com.odde.doughnut.exceptions.ApiException;
import java.util.List;

public enum BookFormat {
  PDF {
    @Override
    public List<ContentLocator> assembleContentLocators(List<BookContentBlock> contentBlocks) {
      return BookBlockPdfContentLocators.pdfContentLocators(contentBlocks);
    }

    @Override
    public void validateAttachRequest(AttachBookRequest request) {
      List<Object> contentList = request.getContentList();
      boolean hasContentList = contentList != null && !contentList.isEmpty();
      AttachBookLayoutRequest layout = request.getLayout();
      List<AttachBookLayoutNodeRequest> layoutRoots =
          layout != null && layout.getRoots() != null ? layout.getRoots() : null;
      boolean hasLayoutRoots = layoutRoots != null && !layoutRoots.isEmpty();

      if (hasContentList && hasLayoutRoots) {
        throw new ApiException(
            "cannot send both layout.roots and contentList",
            ApiError.ErrorType.BINDING_ERROR,
            "cannot send both layout.roots and contentList");
      }
      if (!hasContentList && !hasLayoutRoots) {
        throw new ApiException(
            "exactly one of layout.roots or contentList is required",
            ApiError.ErrorType.BINDING_ERROR,
            "exactly one of layout.roots or contentList is required");
      }

      if (hasContentList) {
        if (contentList.size() > MAX_CONTENT_LIST_ITEMS) {
          throw new ApiException(
              "contentList exceeds maximum size of " + MAX_CONTENT_LIST_ITEMS,
              ApiError.ErrorType.BINDING_ERROR,
              "contentList exceeds maximum size of " + MAX_CONTENT_LIST_ITEMS);
        }
        AttachBookLayoutRequest built = MineruContentListLayoutBuilder.buildLayout(contentList);
        if (built.getRoots().isEmpty()) {
          throw new ApiException(
              "contentList produced no book layout blocks",
              ApiError.ErrorType.BINDING_ERROR,
              "contentList produced no book layout blocks");
        }
        request.setLayout(built);
        layoutRoots = built.getRoots();
      }

      for (AttachBookLayoutNodeRequest root : layoutRoots) {
        validateLayoutNode(root, 1);
      }
    }
  },
  EPUB {
    @Override
    public List<ContentLocator> assembleContentLocators(List<BookContentBlock> contentBlocks) {
      return BookBlockEpubContentLocators.epubContentLocators(contentBlocks);
    }

    @Override
    public void validateAttachRequest(AttachBookRequest request) {
      List<Object> contentList = request.getContentList();
      boolean hasContentList = contentList != null && !contentList.isEmpty();
      AttachBookLayoutRequest layout = request.getLayout();
      List<AttachBookLayoutNodeRequest> layoutRoots =
          layout != null && layout.getRoots() != null ? layout.getRoots() : null;
      boolean hasLayoutRoots = layoutRoots != null && !layoutRoots.isEmpty();

      if (hasContentList || hasLayoutRoots) {
        throw new ApiException(
            "EPUB attach must not include layout or contentList",
            ApiError.ErrorType.BINDING_ERROR,
            "EPUB attach must not include layout or contentList");
      }
    }
  };

  public abstract List<ContentLocator> assembleContentLocators(
      List<BookContentBlock> contentBlocks);

  public abstract void validateAttachRequest(AttachBookRequest request);

  public static BookFormat fromString(String format) {
    if (BookReadingWireConstants.BOOK_FORMAT_EPUB.equals(format)) {
      return EPUB;
    }
    return PDF;
  }

  private static void validateLayoutNode(AttachBookLayoutNodeRequest node, int depth) {
    if (depth > MAX_LAYOUT_DEPTH) {
      throw new ApiException(
          "layout exceeds maximum depth of " + MAX_LAYOUT_DEPTH,
          ApiError.ErrorType.BINDING_ERROR,
          "layout exceeds maximum depth of " + MAX_LAYOUT_DEPTH);
    }
    String title = trimToNull(node.getTitle());
    if (title == null || title.isEmpty()) {
      throw new ApiException(
          "each node title must be non-empty",
          ApiError.ErrorType.BINDING_ERROR,
          "each node title must be non-empty");
    }
    if (title.length() > BookBlockTitleLimits.STRUCTURAL_MAX_CHARS) {
      throw new ApiException(
          "title exceeds maximum length",
          ApiError.ErrorType.BINDING_ERROR,
          "title exceeds maximum length");
    }
    List<AttachBookLayoutNodeRequest> children = node.getChildren();
    if (children != null) {
      for (AttachBookLayoutNodeRequest child : children) {
        validateLayoutNode(child, depth + 1);
      }
    }
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
