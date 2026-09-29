package com.odde.donut.services.book;

import static java.util.stream.Collectors.toSet;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.exceptions.ApiException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extracts EPUB 3 navigation TOC and spine XHTML content into {@link BookBlock} layout rows with
 * {@link BookContentBlock} payloads.
 */
final class EpubStructureExtractor {

  record EpubLayoutBlock(String title, int depth, List<Map<String, Object>> contentPayloads) {}

  private EpubStructureExtractor() {}

  static List<EpubLayoutBlock> extractEpubLayoutWithContent(byte[] epubBytes) {
    EpubPackageDocument pkg = EpubPackageDocument.load(epubBytes);
    if (pkg == null) {
      return List.of();
    }
    List<String> spineXhtmlPaths = pkg.spineXhtmlPaths();

    List<EpubTocEntry> tocEntries = EpubNavDocument.tocEntries(epubBytes, pkg);
    if (tocEntries.isEmpty()) {
      tocEntries = EpubHeadingOutline.tocEntries(epubBytes, spineXhtmlPaths);
    }
    assertTocDepthWithinLimit(tocEntries);

    if (tocEntries.isEmpty()) {
      return List.of();
    }

    List<List<Map<String, Object>>> perBlock = new ArrayList<>();
    for (int i = 0; i < tocEntries.size(); i++) {
      perBlock.add(new ArrayList<>());
    }
    List<Map<String, Object>> orphans = new ArrayList<>();
    Set<String> tocTargets = tocEntries.stream().map(EpubTocEntry::spineZipPath).collect(toSet());
    // Spine files before the first one the table of contents targets, such as a cover page,
    // belong to the *beginning* block; later untargeted files are left out.
    List<String> leadingSpinePaths =
        spineXhtmlPaths.stream().takeWhile(path -> !tocTargets.contains(path)).toList();

    for (String spinePath : spineXhtmlPaths) {
      if (tocTargets.contains(spinePath) || leadingSpinePaths.contains(spinePath)) {
        EpubSpineContent.extract(epubBytes, spinePath, tocEntries, perBlock, orphans);
      }
    }

    List<EpubLayoutBlock> out = new ArrayList<>();
    if (!leadingSpinePaths.isEmpty() || !orphans.isEmpty()) {
      List<Map<String, Object>> beginningPayloads = new ArrayList<>();
      beginningPayloads.add(beginningAnchorPayload(leadingSpinePaths, orphans));
      beginningPayloads.addAll(orphans);
      out.add(new EpubLayoutBlock("*beginning*", 0, List.copyOf(beginningPayloads)));
    }
    for (int i = 0; i < tocEntries.size(); i++) {
      EpubTocEntry entry = tocEntries.get(i);
      out.add(new EpubLayoutBlock(entry.title(), entry.depth(), List.copyOf(perBlock.get(i))));
    }
    return out;
  }

  private static void assertTocDepthWithinLimit(List<EpubTocEntry> tocEntries) {
    for (EpubTocEntry entry : tocEntries) {
      if (entry.depth() > BookReadingWireConstants.MAX_LAYOUT_DEPTH) {
        throw new ApiException(
            "EPUB table of contents exceeds maximum depth of "
                + BookReadingWireConstants.MAX_LAYOUT_DEPTH,
            ApiError.ErrorType.BINDING_ERROR,
            "EPUB table of contents exceeds maximum depth of "
                + BookReadingWireConstants.MAX_LAYOUT_DEPTH);
      }
    }
  }

  /**
   * The *beginning* block starts at the first leading spine file when there is one, otherwise at
   * its first content.
   */
  private static Map<String, Object> beginningAnchorPayload(
      List<String> leadingSpinePaths, List<Map<String, Object>> orphans) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", "beginning_anchor");
    m.put("kind", "beginning");
    if (leadingSpinePaths.isEmpty()) {
      m.put("href", orphans.getFirst().get("href"));
      m.put("fragment", orphans.getFirst().get("fragment"));
    } else {
      m.put("href", leadingSpinePaths.getFirst());
      m.put("fragment", "");
    }
    return m;
  }
}
