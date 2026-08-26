package com.odde.donut.services.book;

import static com.odde.donut.services.book.BookReadingWireConstants.MAX_CONTENT_LIST_ITEMS;
import static com.odde.donut.services.book.BookReadingWireConstants.MAX_LAYOUT_DEPTH;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.donut.controllers.dto.AttachBookLayoutRequest;
import com.odde.donut.controllers.dto.AttachBookRequest;
import com.odde.donut.entities.BookBlockTitleLimits;
import com.odde.donut.exceptions.ApiException;
import java.util.List;

final class AttachBookLayoutValidator {

  private AttachBookLayoutValidator() {}

  static void validatePdfAttachRequest(AttachBookRequest request) {
    List<Object> contentList = request.getContentList();
    boolean hasContentList = hasContentList(request);
    List<AttachBookLayoutNodeRequest> layoutRoots = layoutRootsOrNull(request);
    boolean hasLayoutRoots = hasLayoutRoots(layoutRoots);

    if (hasContentList && hasLayoutRoots) {
      throw bindingError("cannot send both book layout roots and contentList");
    }
    if (!hasContentList && !hasLayoutRoots) {
      throw bindingError("exactly one of book layout roots or contentList is required");
    }

    if (hasContentList) {
      if (contentList.size() > MAX_CONTENT_LIST_ITEMS) {
        throw bindingError("contentList exceeds maximum size of " + MAX_CONTENT_LIST_ITEMS);
      }
      AttachBookLayoutRequest built = MineruContentListLayoutBuilder.buildLayout(contentList);
      if (built.getRoots().isEmpty()) {
        throw bindingError("contentList produced no book layout blocks");
      }
      request.setBookLayout(built);
      layoutRoots = built.getRoots();
    }

    for (AttachBookLayoutNodeRequest root : layoutRoots) {
      validateLayoutNode(root, 1);
    }
  }

  static void validateEpubAttachRequest(AttachBookRequest request) {
    if (hasContentList(request) || hasLayoutRoots(layoutRootsOrNull(request))) {
      throw bindingError("EPUB attach must not include book layout or contentList");
    }
  }

  static void rejectIfExceedsMaxDepth(int depth) {
    if (depth > MAX_LAYOUT_DEPTH) {
      throw bindingError("book layout exceeds maximum depth of " + MAX_LAYOUT_DEPTH);
    }
  }

  private static void validateLayoutNode(AttachBookLayoutNodeRequest node, int depth) {
    rejectIfExceedsMaxDepth(depth);
    String title = trimToNull(node.getTitle());
    if (title == null || title.isEmpty()) {
      throw bindingError("each node title must be non-empty");
    }
    if (title.length() > BookBlockTitleLimits.STRUCTURAL_MAX_CHARS) {
      throw bindingError("title exceeds maximum length");
    }
    List<AttachBookLayoutNodeRequest> children = node.getChildren();
    if (children != null) {
      for (AttachBookLayoutNodeRequest child : children) {
        validateLayoutNode(child, depth + 1);
      }
    }
  }

  private static boolean hasContentList(AttachBookRequest request) {
    List<Object> contentList = request.getContentList();
    return contentList != null && !contentList.isEmpty();
  }

  private static boolean hasLayoutRoots(List<AttachBookLayoutNodeRequest> layoutRoots) {
    return layoutRoots != null && !layoutRoots.isEmpty();
  }

  private static List<AttachBookLayoutNodeRequest> layoutRootsOrNull(AttachBookRequest request) {
    AttachBookLayoutRequest layout = request.getBookLayout();
    return layout != null && layout.getRoots() != null ? layout.getRoots() : null;
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static ApiException bindingError(String message) {
    return new ApiException(message, ApiError.ErrorType.BINDING_ERROR, message);
  }
}
