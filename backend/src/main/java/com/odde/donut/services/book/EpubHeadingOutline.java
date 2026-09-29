package com.odde.donut.services.book;

import static com.odde.donut.services.book.EpubXhtml.loadBody;
import static com.odde.donut.services.book.EpubXhtml.preorder;
import static com.odde.donut.services.book.EpubXhtml.textContent;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Table-of-contents entries from spine headings, for EPUBs without a navigation document. */
final class EpubHeadingOutline {

  private EpubHeadingOutline() {}

  static List<EpubTocEntry> tocEntries(byte[] epubBytes, List<String> spineXhtmlPaths) {
    List<EpubTocEntry> out = new ArrayList<>();
    for (String spinePath : spineXhtmlPaths) {
      Element body = loadBody(epubBytes, spinePath);
      if (body == null) {
        continue;
      }
      IdentityHashMap<Element, Integer> preorder = preorder(body);
      List<Integer> levelStack = new ArrayList<>();
      NodeList children = body.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node n = children.item(i);
        if (n instanceof Element child) {
          walkElementForHeadings(child, spinePath, levelStack, out, preorder);
        }
      }
    }
    return out;
  }

  private static boolean isHeadingTag(String localName) {
    if (localName == null || localName.length() != 2 || localName.charAt(0) != 'h') {
      return false;
    }
    char c = localName.charAt(1);
    return c >= '1' && c <= '6';
  }

  private static void walkElementForHeadings(
      Element el,
      String spineZipPath,
      List<Integer> levelStack,
      List<EpubTocEntry> out,
      IdentityHashMap<Element, Integer> preorder) {
    String local = el.getLocalName();
    if (isHeadingTag(local)) {
      int level = local.charAt(1) - '0';
      while (!levelStack.isEmpty() && levelStack.getLast() >= level) {
        levelStack.removeLast();
      }
      levelStack.add(level);
      int depth = levelStack.size() - 1;
      String title = textContent(el);
      if (!title.isBlank()) {
        String id = el.getAttribute("id");
        String frag = (id != null && !id.isBlank()) ? id.trim() : null;
        Integer startIfNoFrag = frag == null ? preorder.get(el) : null;
        out.add(new EpubTocEntry(title, depth, spineZipPath, frag, startIfNoFrag));
      }
    }
    NodeList children = el.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n instanceof Element child) {
        walkElementForHeadings(child, spineZipPath, levelStack, out, preorder);
      }
    }
  }
}
