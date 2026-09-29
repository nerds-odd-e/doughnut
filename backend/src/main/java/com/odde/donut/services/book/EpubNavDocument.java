package com.odde.donut.services.book;

import static com.odde.donut.services.book.EpubPackageIo.MAX_NAV_BYTES;
import static com.odde.donut.services.book.EpubPackageIo.parseXmlSecure;
import static com.odde.donut.services.book.EpubPackageIo.readEntryBytes;
import static com.odde.donut.services.book.EpubPackageIo.readError;
import static com.odde.donut.services.book.EpubXhtml.NS_XHTML;
import static com.odde.donut.services.book.EpubXhtml.firstChildByLocal;
import static com.odde.donut.services.book.EpubXhtml.textContent;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.exceptions.ApiException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Table-of-contents entries from the EPUB 3 navigation document's {@code toc} nav. */
final class EpubNavDocument {

  private static final String NS_EPUB = "http://www.idpf.org/2007/ops";

  private EpubNavDocument() {}

  /** Entries in reading order; empty when the package has no usable navigation document. */
  static List<EpubTocEntry> tocEntries(byte[] epubBytes, EpubPackageDocument pkg) {
    String navHref = pkg.navHref();
    if (navHref == null) {
      return List.of();
    }
    String navZipPath = pkg.resolve(navHref);
    byte[] navBytes;
    try {
      navBytes = readEntryBytes(epubBytes, navZipPath, MAX_NAV_BYTES);
    } catch (IOException e) {
      throw readError();
    }
    if (navBytes == null) {
      return List.of();
    }
    Document navDoc;
    try {
      navDoc = parseXmlSecure(navBytes);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new ApiException(
          "EPUB navigation document is invalid or unreadable",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB navigation document is invalid or unreadable");
    }
    Element tocNav = findTocNavElement(navDoc);
    if (tocNav == null) {
      return List.of();
    }
    Element rootOl = firstChildByLocal(tocNav, "ol");
    if (rootOl == null) {
      return List.of();
    }
    List<EpubTocEntry> entries = new ArrayList<>();
    walkOl(rootOl, 0, navZipPath, entries);
    return entries;
  }

  static ApiException hrefNotAllowed() {
    return new ApiException(
        "EPUB navigation href is not allowed",
        ApiError.ErrorType.BINDING_ERROR,
        "EPUB navigation href is not allowed");
  }

  /** Resolves a TOC {@code href} to a ZIP path; fragment (if any) is ignored. */
  private static String resolveAgainstNavDoc(String navZipPath, String hrefTrimmed) {
    String href = hrefTrimmed.trim();
    int hash = href.indexOf('#');
    String pathPart = hash >= 0 ? href.substring(0, hash) : href;
    if (pathPart.contains("..")) {
      throw hrefNotAllowed();
    }
    Path navPath = Paths.get(navZipPath.replace('\\', '/'));
    Path navParent = navPath.getParent() != null ? navPath.getParent() : Paths.get("");
    if (pathPart.isEmpty()) {
      return navZipPath.replace('\\', '/');
    }
    Path resolved = navParent.resolve(pathPart).normalize();
    if (resolved.isAbsolute()) {
      throw hrefNotAllowed();
    }
    return resolved.toString().replace('\\', '/');
  }

  private static EpubTocEntry tocEntryFromAnchor(Element anchor, int depth, String navZipPath) {
    String hrefRaw = anchor.getAttribute("href");
    if (hrefRaw == null || hrefRaw.isBlank()) {
      return null;
    }
    String href = hrefRaw.trim();
    int hash = href.indexOf('#');
    String frag = null;
    if (hash >= 0 && hash + 1 < href.length()) {
      String f = href.substring(hash + 1).trim();
      if (!f.isEmpty()) {
        frag = f;
      }
    }
    String spineZip = resolveAgainstNavDoc(navZipPath, href);
    String title = textContent(anchor);
    if (title.isBlank()) {
      return null;
    }
    return new EpubTocEntry(title, depth, spineZip, frag, null);
  }

  private static Element findTocNavElement(Document doc) {
    NodeList navs = doc.getElementsByTagNameNS(NS_XHTML, "nav");
    if (navs.getLength() == 0) {
      navs = doc.getElementsByTagName("nav");
    }
    for (int i = 0; i < navs.getLength(); i++) {
      Node n = navs.item(i);
      if (n instanceof Element nav && isTocNav(nav)) {
        return nav;
      }
    }
    return null;
  }

  private static boolean isTocNav(Element nav) {
    String typeNs = nav.getAttributeNS(NS_EPUB, "type");
    if ("toc".equals(typeNs)) {
      return true;
    }
    String legacy = nav.getAttribute("epub:type");
    return "toc".equals(legacy);
  }

  private static void walkOl(Element ol, int itemDepth, String navZipPath, List<EpubTocEntry> out) {
    NodeList children = ol.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (!(n instanceof Element li) || !"li".equals(li.getLocalName())) {
        continue;
      }
      Element anchor = firstAnchorInLi(li);
      if (anchor != null) {
        EpubTocEntry entry = tocEntryFromAnchor(anchor, itemDepth, navZipPath);
        if (entry != null) {
          out.add(entry);
        }
      }
      Element nestedOl = firstChildByLocal(li, "ol");
      if (nestedOl != null) {
        walkOl(nestedOl, itemDepth + 1, navZipPath, out);
      }
    }
  }

  private static Element firstAnchorInLi(Element li) {
    NodeList nodes = li.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);
      if (n instanceof Element el && isAnchor(el)) {
        return el;
      }
    }
    NodeList anchors = li.getElementsByTagNameNS(NS_XHTML, "a");
    if (anchors.getLength() == 0) {
      anchors = li.getElementsByTagName("a");
    }
    if (anchors.getLength() > 0 && anchors.item(0) instanceof Element a) {
      return a;
    }
    return null;
  }

  private static boolean isAnchor(Element el) {
    return "a".equals(el.getLocalName())
        && (NS_XHTML.equals(el.getNamespaceURI()) || el.getNamespaceURI() == null);
  }
}
