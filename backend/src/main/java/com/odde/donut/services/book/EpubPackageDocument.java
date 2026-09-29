package com.odde.donut.services.book;

import static com.odde.donut.services.book.EpubPackageIo.MAX_CONTAINER_BYTES;
import static com.odde.donut.services.book.EpubPackageIo.MAX_OPF_BYTES;
import static com.odde.donut.services.book.EpubPackageIo.NS_OPF;
import static com.odde.donut.services.book.EpubPackageIo.parseContainerRootfileFullPath;
import static com.odde.donut.services.book.EpubPackageIo.parseXmlSecure;
import static com.odde.donut.services.book.EpubPackageIo.readEntryBytes;
import static com.odde.donut.services.book.EpubPackageIo.readError;
import static com.odde.donut.services.book.EpubXhtml.firstChildByLocal;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.exceptions.ApiException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** The EPUB package (OPF) document: its manifest, spine, and navigation document reference. */
record EpubPackageDocument(String opfEntryName, Element packageEl) {

  private record ManifestItem(String zipPath, String mediaType) {}

  /** The package document named by {@code META-INF/container.xml}, or null when absent. */
  static EpubPackageDocument load(byte[] epubBytes) {
    byte[] containerBytes;
    try {
      containerBytes = readEntryBytes(epubBytes, "META-INF/container.xml", MAX_CONTAINER_BYTES);
    } catch (IOException e) {
      throw readError();
    }
    if (containerBytes == null) {
      return null;
    }
    String opfRelative = parseContainerRootfileFullPath(containerBytes);
    if (opfRelative == null || opfRelative.isBlank()) {
      return null;
    }
    String opfSlashes = opfRelative.replace('\\', '/');
    if (opfSlashes.contains("..")) {
      throw packagePathNotAllowed();
    }
    Path normalizedOpf = Paths.get(opfSlashes).normalize();
    if (normalizedOpf.isAbsolute()) {
      throw packagePathNotAllowed();
    }
    String opfEntryName = normalizedOpf.toString().replace('\\', '/');
    byte[] opfBytes;
    try {
      opfBytes = readEntryBytes(epubBytes, opfEntryName, MAX_OPF_BYTES);
    } catch (IOException e) {
      throw readError();
    }
    if (opfBytes == null) {
      return null;
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
    return new EpubPackageDocument(opfEntryName, packageEl);
  }

  /** ZIP paths of the XHTML spine items, in reading order. */
  List<String> spineXhtmlPaths() {
    Element manifestEl = firstChildByLocal(packageEl, "manifest");
    Element spineEl = firstChildByLocal(packageEl, "spine");
    if (manifestEl == null || spineEl == null) {
      return List.of();
    }
    Map<String, ManifestItem> byId = manifestItems(manifestEl);
    NodeList itemrefs = spineEl.getElementsByTagNameNS(NS_OPF, "itemref");
    if (itemrefs.getLength() == 0) {
      itemrefs = spineEl.getElementsByTagName("itemref");
    }
    List<String> paths = new ArrayList<>();
    for (int i = 0; i < itemrefs.getLength(); i++) {
      Node n = itemrefs.item(i);
      if (!(n instanceof Element ir)) {
        continue;
      }
      String idref = ir.getAttribute("idref");
      if (idref == null || idref.isBlank()) {
        continue;
      }
      ManifestItem item = byId.get(idref.trim());
      if (item == null || !isXhtmlMediaType(item.mediaType())) {
        continue;
      }
      paths.add(item.zipPath());
    }
    return paths;
  }

  private Map<String, ManifestItem> manifestItems(Element manifestEl) {
    Map<String, ManifestItem> map = new LinkedHashMap<>();
    NodeList items = manifestEl.getElementsByTagNameNS(NS_OPF, "item");
    if (items.getLength() == 0) {
      items = manifestEl.getElementsByTagName("item");
    }
    for (int i = 0; i < items.getLength(); i++) {
      Node n = items.item(i);
      if (!(n instanceof Element item)) {
        continue;
      }
      String id = item.getAttribute("id");
      String href = item.getAttribute("href");
      if (id == null || id.isBlank() || href == null || href.isBlank()) {
        continue;
      }
      String mt = item.getAttribute("media-type");
      map.put(id.trim(), new ManifestItem(resolve(href), mt == null ? "" : mt.trim()));
    }
    return map;
  }

  private static boolean isXhtmlMediaType(String mediaType) {
    if (mediaType == null || mediaType.isEmpty()) {
      return false;
    }
    return "application/xhtml+xml".equalsIgnoreCase(mediaType)
        || "application/xml".equalsIgnoreCase(mediaType)
        || mediaType.toLowerCase().contains("html");
  }

  /** The manifest {@code href} of the navigation document, or null when there is none. */
  String navHref() {
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

  /** Resolves a package-relative {@code href} to a ZIP path; any fragment is ignored. */
  String resolve(String hrefRaw) {
    String href = hrefRaw.trim();
    int hash = href.indexOf('#');
    String pathPart = hash >= 0 ? href.substring(0, hash) : href;
    if (pathPart.contains("..")) {
      throw EpubNavDocument.hrefNotAllowed();
    }
    Path opfPath = Paths.get(opfEntryName.replace('\\', '/'));
    Path opfParent = opfPath.getParent() != null ? opfPath.getParent() : Paths.get("");
    Path resolved = opfParent.resolve(pathPart).normalize();
    if (resolved.isAbsolute()) {
      throw EpubNavDocument.hrefNotAllowed();
    }
    return resolved.toString().replace('\\', '/');
  }

  private static ApiException packagePathNotAllowed() {
    return new ApiException(
        "EPUB package path is not allowed",
        ApiError.ErrorType.BINDING_ERROR,
        "EPUB package path is not allowed");
  }

  private static ApiException invalidOpf() {
    return new ApiException(
        "EPUB package document is invalid or unreadable",
        ApiError.ErrorType.BINDING_ERROR,
        "EPUB package document is invalid or unreadable");
  }
}
