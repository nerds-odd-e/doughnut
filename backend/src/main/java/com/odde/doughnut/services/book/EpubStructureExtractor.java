package com.odde.doughnut.services.book;

import static com.odde.doughnut.services.book.EpubPackageIo.MAX_CONTAINER_BYTES;
import static com.odde.doughnut.services.book.EpubPackageIo.MAX_NAV_BYTES;
import static com.odde.doughnut.services.book.EpubPackageIo.MAX_OPF_BYTES;
import static com.odde.doughnut.services.book.EpubPackageIo.NS_OPF;
import static com.odde.doughnut.services.book.EpubPackageIo.parseContainerRootfileFullPath;
import static com.odde.doughnut.services.book.EpubPackageIo.parseXmlSecure;
import static com.odde.doughnut.services.book.EpubPackageIo.readEntryBytes;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.exceptions.ApiException;
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

/**
 * Extracts the EPUB 3 navigation TOC (<code>nav</code> with <code>epub:type="toc"</code>) into a
 * preorder outline (titles and depths). When no nav manifest item exists, returns an empty list.
 */
final class EpubStructureExtractor {

  private static final String NS_EPUB = "http://www.idpf.org/2007/ops";
  private static final String NS_XHTML = "http://www.w3.org/1999/xhtml";

  record NavOutlineEntry(String title, int depth) {}

  private EpubStructureExtractor() {}

  static List<NavOutlineEntry> extractNavOutline(byte[] epubBytes) {
    byte[] containerBytes;
    try {
      containerBytes = readEntryBytes(epubBytes, "META-INF/container.xml", MAX_CONTAINER_BYTES);
    } catch (IOException e) {
      throw new ApiException(
          "EPUB file could not be read",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB file could not be read");
    }
    if (containerBytes == null) {
      return List.of();
    }
    String opfRelative = parseContainerRootfileFullPath(containerBytes);
    if (opfRelative == null || opfRelative.isBlank()) {
      return List.of();
    }
    String opfSlashes = opfRelative.replace('\\', '/');
    if (opfSlashes.contains("..")) {
      throw new ApiException(
          "EPUB package path is not allowed",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB package path is not allowed");
    }
    Path normalizedOpf = Paths.get(opfSlashes).normalize();
    if (normalizedOpf.isAbsolute()) {
      throw new ApiException(
          "EPUB package path is not allowed",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB package path is not allowed");
    }
    String opfEntryName = normalizedOpf.toString().replace('\\', '/');
    byte[] opfBytes;
    try {
      opfBytes = readEntryBytes(epubBytes, opfEntryName, MAX_OPF_BYTES);
    } catch (IOException e) {
      throw new ApiException(
          "EPUB file could not be read",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB file could not be read");
    }
    if (opfBytes == null) {
      return List.of();
    }
    Document opfDoc;
    try {
      opfDoc = parseXmlSecure(opfBytes);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw invalidOpf();
    }
    Element packageEl = opfDoc.getDocumentElement();
    if (packageEl == null || !"package".equals(packageEl.getLocalName())) {
      throw invalidOpf();
    }
    String navHref = findNavManifestHref(packageEl);
    if (navHref == null) {
      return List.of();
    }
    String navZipPath = resolveAgainstOpf(opfEntryName, navHref);
    byte[] navBytes;
    try {
      navBytes = readEntryBytes(epubBytes, navZipPath, MAX_NAV_BYTES);
    } catch (IOException e) {
      throw new ApiException(
          "EPUB file could not be read",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB file could not be read");
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
    Element rootOl = firstChildElement(tocNav, "ol");
    if (rootOl == null) {
      return List.of();
    }
    List<NavOutlineEntry> out = new ArrayList<>();
    walkOl(rootOl, 0, out);
    for (NavOutlineEntry e : out) {
      if (e.depth() > BookReadingWireConstants.MAX_LAYOUT_DEPTH) {
        throw new ApiException(
            "EPUB table of contents exceeds maximum depth of "
                + BookReadingWireConstants.MAX_LAYOUT_DEPTH,
            ApiError.ErrorType.BINDING_ERROR,
            "EPUB table of contents exceeds maximum depth of "
                + BookReadingWireConstants.MAX_LAYOUT_DEPTH);
      }
    }
    return out;
  }

  private static String findNavManifestHref(Element packageEl) {
    NodeList manifests = packageEl.getElementsByTagNameNS(NS_OPF, "manifest");
    if (manifests.getLength() == 0) {
      manifests = packageEl.getElementsByTagName("manifest");
    }
    if (manifests.getLength() == 0) {
      return null;
    }
    Element manifest = (Element) manifests.item(0);
    NodeList items = manifest.getElementsByTagNameNS(NS_OPF, "item");
    if (items.getLength() == 0) {
      items = manifest.getElementsByTagName("item");
    }
    for (int i = 0; i < items.getLength(); i++) {
      Node n = items.item(i);
      if (n instanceof Element item && hasNavProperty(item.getAttribute("properties"))) {
        String href = item.getAttribute("href");
        if (href != null && !href.isBlank()) {
          return href.trim();
        }
      }
    }
    return null;
  }

  private static boolean hasNavProperty(String propertiesAttr) {
    if (propertiesAttr == null || propertiesAttr.isBlank()) {
      return false;
    }
    for (String t : propertiesAttr.trim().split("\\s+")) {
      if ("nav".equals(t)) {
        return true;
      }
    }
    return false;
  }

  private static String resolveAgainstOpf(String opfEntryName, String hrefRaw) {
    String href = hrefRaw.trim();
    int hash = href.indexOf('#');
    String pathPart = hash >= 0 ? href.substring(0, hash) : href;
    if (pathPart.contains("..")) {
      throw new ApiException(
          "EPUB navigation href is not allowed",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB navigation href is not allowed");
    }
    Path opfPath = Paths.get(opfEntryName.replace('\\', '/'));
    Path opfParent = opfPath.getParent() != null ? opfPath.getParent() : Paths.get("");
    Path resolved = opfParent.resolve(pathPart).normalize();
    if (resolved.isAbsolute()) {
      throw new ApiException(
          "EPUB navigation href is not allowed",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB navigation href is not allowed");
    }
    return resolved.toString().replace('\\', '/');
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

  private static void walkOl(Element ol, int itemDepth, List<NavOutlineEntry> out) {
    NodeList children = ol.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (!(n instanceof Element li) || !"li".equals(li.getLocalName())) {
        continue;
      }
      String title = firstMeaningfulAnchorText(li);
      Element nestedOl = firstDirectChildOl(li);
      if (title != null && !title.isBlank()) {
        out.add(new NavOutlineEntry(title.trim(), itemDepth));
      }
      if (nestedOl != null) {
        walkOl(nestedOl, itemDepth + 1, out);
      }
    }
  }

  private static String firstMeaningfulAnchorText(Element li) {
    NodeList nodes = li.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);
      if (n instanceof Element el && isAnchor(el)) {
        return textContent(el);
      }
    }
    NodeList anchors = li.getElementsByTagNameNS(NS_XHTML, "a");
    if (anchors.getLength() == 0) {
      anchors = li.getElementsByTagName("a");
    }
    if (anchors.getLength() > 0 && anchors.item(0) instanceof Element a) {
      return textContent(a);
    }
    return null;
  }

  private static boolean isAnchor(Element el) {
    return "a".equals(el.getLocalName())
        && (NS_XHTML.equals(el.getNamespaceURI()) || el.getNamespaceURI() == null);
  }

  private static Element firstDirectChildOl(Element li) {
    NodeList nodes = li.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);
      if (n instanceof Element el && "ol".equals(el.getLocalName())) {
        return el;
      }
    }
    return null;
  }

  private static String textContent(Element el) {
    return el.getTextContent() != null ? el.getTextContent().trim() : "";
  }

  private static Element firstChildElement(Element parent, String localName) {
    NodeList nodes = parent.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);
      if (n instanceof Element el && localName.equals(el.getLocalName())) {
        return el;
      }
    }
    return null;
  }

  private static ApiException invalidOpf() {
    return new ApiException(
        "EPUB package document is invalid or unreadable",
        ApiError.ErrorType.BINDING_ERROR,
        "EPUB package document is invalid or unreadable");
  }
}
