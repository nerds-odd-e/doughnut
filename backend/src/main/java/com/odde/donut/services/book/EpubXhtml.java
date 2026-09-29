package com.odde.donut.services.book;

import static com.odde.donut.services.book.EpubPackageIo.MAX_SPINE_XHTML_BYTES;
import static com.odde.donut.services.book.EpubPackageIo.parseXmlSecure;
import static com.odde.donut.services.book.EpubPackageIo.readEntryBytes;
import static com.odde.donut.services.book.EpubPackageIo.readError;

import java.io.IOException;
import java.util.IdentityHashMap;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** DOM helpers for the XHTML documents inside an EPUB. */
final class EpubXhtml {

  static final String NS_XHTML = "http://www.w3.org/1999/xhtml";

  private EpubXhtml() {}

  /** The body of a spine XHTML file, or null when the file is missing or not well-formed. */
  static Element loadBody(byte[] epubBytes, String spineZipPath) {
    byte[] xhtmlBytes;
    try {
      xhtmlBytes = readEntryBytes(epubBytes, spineZipPath, MAX_SPINE_XHTML_BYTES);
    } catch (IOException e) {
      throw readError();
    }
    if (xhtmlBytes == null) {
      return null;
    }
    try {
      return findBody(parseXmlSecure(xhtmlBytes));
    } catch (ParserConfigurationException | SAXException | IOException e) {
      return null;
    }
  }

  private static Element findBody(Document doc) {
    NodeList bodies = doc.getElementsByTagNameNS(NS_XHTML, "body");
    if (bodies.getLength() == 0) {
      bodies = doc.getElementsByTagName("body");
    }
    if (bodies.getLength() > 0 && bodies.item(0) instanceof Element el) {
      return el;
    }
    return null;
  }

  /** Document-order position of every element under {@code root}, starting at 0. */
  static IdentityHashMap<Element, Integer> preorder(Element root) {
    IdentityHashMap<Element, Integer> map = new IdentityHashMap<>();
    assignPreorder(root, new int[] {0}, map);
    return map;
  }

  private static void assignPreorder(
      Element el, int[] counter, IdentityHashMap<Element, Integer> map) {
    map.put(el, counter[0]++);
    NodeList children = el.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n instanceof Element child) {
        assignPreorder(child, counter, map);
      }
    }
  }

  /**
   * XHTML in EPUB often has no DTD declaring {@code id} as type ID, so {@link
   * Document#getElementById} may return null; fall back to scanning {@code id} attributes.
   */
  static Element findElementByIdAttr(Document doc, String id) {
    if (id == null || id.isBlank()) {
      return null;
    }
    Element dom = doc.getElementById(id);
    if (dom != null) {
      return dom;
    }
    Element root = doc.getDocumentElement();
    return root == null ? null : findElementByIdAttrDepthFirst(root, id);
  }

  private static Element findElementByIdAttrDepthFirst(Element el, String id) {
    String a = el.getAttribute("id");
    if (id.equals(a != null ? a : "")) {
      return el;
    }
    NodeList children = el.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n instanceof Element child) {
        Element found = findElementByIdAttrDepthFirst(child, id);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  static Element firstChildByLocal(Element parent, String localName) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n instanceof Element el && localName.equals(el.getLocalName())) {
        return el;
      }
    }
    return null;
  }

  static String textContent(Element el) {
    return el.getTextContent() != null ? el.getTextContent().trim() : "";
  }
}
