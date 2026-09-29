package com.odde.donut.services.book;

import static com.odde.donut.services.book.EpubXhtml.findElementByIdAttr;
import static com.odde.donut.services.book.EpubXhtml.loadBody;
import static com.odde.donut.services.book.EpubXhtml.preorder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Distributes one spine XHTML file's paragraphs, images, and tables to the table-of-contents
 * entries that start before them; content before every entry in the file goes to {@code orphans}.
 */
final class EpubSpineContent {

  private static final int DOC_START_PREORDER = -1;

  private record SectionStart(int tocIndex, int startPreorder) {}

  private final String spineZipPath;
  private final IdentityHashMap<Element, Integer> preorder;
  private final List<SectionStart> sections;
  private final List<List<Map<String, Object>>> perBlock;
  private final List<Map<String, Object>> orphans;

  private EpubSpineContent(
      String spineZipPath,
      IdentityHashMap<Element, Integer> preorder,
      List<SectionStart> sections,
      List<List<Map<String, Object>>> perBlock,
      List<Map<String, Object>> orphans) {
    this.spineZipPath = spineZipPath;
    this.preorder = preorder;
    this.sections = sections;
    this.perBlock = perBlock;
    this.orphans = orphans;
  }

  /**
   * Appends the file's content payloads to {@code perBlock} (indexed like {@code tocEntries}) or to
   * {@code orphans}.
   */
  static void extract(
      byte[] epubBytes,
      String spineZipPath,
      List<EpubTocEntry> tocEntries,
      List<List<Map<String, Object>>> perBlock,
      List<Map<String, Object>> orphans) {
    Element body = loadBody(epubBytes, spineZipPath);
    if (body == null) {
      return;
    }
    List<Integer> targeting = new ArrayList<>();
    for (int i = 0; i < tocEntries.size(); i++) {
      if (spineZipPath.equals(tocEntries.get(i).spineZipPath())) {
        targeting.add(i);
      }
    }
    IdentityHashMap<Element, Integer> preorder = preorder(body);
    List<SectionStart> sections = new ArrayList<>();
    for (int tocIdx : targeting) {
      EpubTocEntry entry = tocEntries.get(tocIdx);
      String frag = entry.fragmentId();
      Integer start;
      if (frag == null || frag.isEmpty()) {
        start = entry.startPreorderIfNoFragment();
      } else {
        Element targetEl = findElementByIdAttr(body.getOwnerDocument(), frag);
        start = targetEl == null ? null : preorder.get(targetEl);
      }
      sections.add(new SectionStart(tocIdx, start != null ? start : DOC_START_PREORDER));
    }
    sections.sort(
        Comparator.comparingInt(SectionStart::startPreorder)
            .thenComparingInt(SectionStart::tocIndex));

    new EpubSpineContent(spineZipPath, preorder, sections, perBlock, orphans).emitChildren(body);

    for (int tocIdx : targeting) {
      String fragId = tocEntries.get(tocIdx).fragmentId();
      String fragment = fragId == null ? "" : fragId.trim();
      List<Map<String, Object>> payloads = perBlock.get(tocIdx);
      if (payloads.isEmpty()) {
        // No content of its own (empty, or sharing another entry's start): start at its target.
        payloads.add(startAnchorPayload(spineZipPath, fragment));
      } else if (!fragment.isEmpty()) {
        payloads.getFirst().put("fragment", fragment);
      }
    }
  }

  /**
   * A payload that gives a block its start without contributing content. Its stored type keeps the
   * name {@code beginning_anchor} (shared with PDF layouts and already-attached books) although it
   * now also starts table-of-contents entries with no content of their own.
   */
  static Map<String, Object> startAnchorPayload(String href, String fragment) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", "beginning_anchor");
    m.put("href", href);
    m.put("fragment", fragment);
    return m;
  }

  private void emitChildren(Element el) {
    NodeList children = el.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n instanceof Element child) {
        emitFromElement(child);
      }
    }
  }

  private void emitFromElement(Element el) {
    switch (el.getLocalName()) {
      case "p" -> emitParagraphIfNonEmpty(el);
      case "img" -> emitImage(el);
      case "table" -> emitTable(el);
      case null, default -> emitChildren(el);
    }
  }

  private void emitParagraphIfNonEmpty(Element p) {
    String text = p.getTextContent() != null ? p.getTextContent().trim() : "";
    if (text.isEmpty()) {
      emitChildren(p);
      return;
    }
    Integer po = preorder.get(p);
    if (po == null) {
      return;
    }
    int owner = ownerTocIndex(po);
    Map<String, Object> payload = contentMap("text", fragmentFor(p), text, null);
    if (owner < 0) {
      orphans.add(payload);
      return;
    }
    perBlock.get(owner).add(payload);
    emitChildren(p);
  }

  private void emitImage(Element img) {
    Integer po = preorder.get(img);
    if (po == null) {
      return;
    }
    String src = img.getAttribute("src");
    destination(ownerTocIndex(po))
        .add(contentMap("image", fragmentFor(img), null, src == null ? "" : src.trim()));
  }

  private void emitTable(Element table) {
    Integer po = preorder.get(table);
    if (po == null) {
      return;
    }
    String text = table.getTextContent() != null ? table.getTextContent().trim() : "";
    destination(ownerTocIndex(po)).add(contentMap("table", fragmentFor(table), text, null));
  }

  private List<Map<String, Object>> destination(int ownerTocIndex) {
    return ownerTocIndex < 0 ? orphans : perBlock.get(ownerTocIndex);
  }

  private int ownerTocIndex(int contentPreorder) {
    if (sections.isEmpty()) {
      return -1;
    }
    SectionStart first = sections.getFirst();
    if (first.startPreorder() > DOC_START_PREORDER && contentPreorder < first.startPreorder()) {
      return -1;
    }
    int owner = first.tocIndex();
    for (SectionStart s : sections) {
      if (s.startPreorder() <= contentPreorder) {
        owner = s.tocIndex();
      }
    }
    return owner;
  }

  private static String fragmentFor(Element el) {
    String id = el.getAttribute("id");
    if (id == null || id.isBlank()) {
      return "";
    }
    return id.trim();
  }

  private Map<String, Object> contentMap(String type, String fragment, String text, String src) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", type);
    m.put("href", spineZipPath);
    m.put("fragment", fragment);
    if (text != null) {
      m.put("text", text);
    }
    if (src != null) {
      m.put("src", src);
    }
    return m;
  }
}
