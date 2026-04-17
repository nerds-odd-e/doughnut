package com.odde.doughnut.services.book;

import static com.odde.doughnut.services.book.EpubPackageIo.MAX_CONTAINER_BYTES;
import static com.odde.doughnut.services.book.EpubPackageIo.MAX_NAV_BYTES;
import static com.odde.doughnut.services.book.EpubPackageIo.MAX_OPF_BYTES;
import static com.odde.doughnut.services.book.EpubPackageIo.MAX_SPINE_XHTML_BYTES;
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
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Extracts EPUB 3 navigation TOC and spine XHTML content into {@link BookBlock} layout rows with
 * {@link BookContentBlock} payloads.
 */
final class EpubStructureExtractor {

  private static final String NS_EPUB = "http://www.idpf.org/2007/ops";
  private static final String NS_XHTML = "http://www.w3.org/1999/xhtml";

  private static final int DOC_START_PREORDER = -1;

  record EpubLayoutBlock(String title, int depth, List<Map<String, Object>> contentPayloads) {}

  private record NavRow(
      String title,
      int depth,
      String spineZipPath,
      String fragmentId,
      Integer startPreorderIfNoFragment) {}

  private record SectionStart(int navIndex, int startPreorder) {}

  private EpubStructureExtractor() {}

  static List<EpubLayoutBlock> extractEpubLayoutWithContent(byte[] epubBytes) {
    OpfContext ctx = loadOpfContext(epubBytes);
    if (ctx == null) {
      return List.of();
    }
    List<String> spineXhtmlPaths = spineXhtmlPaths(ctx);

    List<NavRow> navRows = new ArrayList<>();
    String navHref = findNavManifestHref(ctx.packageEl());
    if (navHref != null) {
      navRows.addAll(navRowsFromNavDocument(epubBytes, ctx, navHref));
    }
    if (navRows.isEmpty()) {
      navRows = navRowsFromHeadings(epubBytes, spineXhtmlPaths);
    }
    assertNavDepthWithinLimit(navRows);

    if (navRows.isEmpty()) {
      return List.of();
    }

    List<List<Map<String, Object>>> perBlock = new ArrayList<>();
    for (int i = 0; i < navRows.size(); i++) {
      perBlock.add(new ArrayList<>());
    }
    List<Map<String, Object>> orphans = new ArrayList<>();

    for (String spinePath : spineXhtmlPaths) {
      extractContentForSpineFile(epubBytes, spinePath, navRows, perBlock, orphans);
    }

    List<EpubLayoutBlock> out = new ArrayList<>();
    if (!orphans.isEmpty()) {
      List<Map<String, Object>> beginningPayloads = new ArrayList<>();
      beginningPayloads.add(beginningAnchorPayload(orphans.getFirst()));
      beginningPayloads.addAll(orphans);
      out.add(new EpubLayoutBlock("*beginning*", 0, List.copyOf(beginningPayloads)));
    }
    for (int i = 0; i < navRows.size(); i++) {
      NavRow row = navRows.get(i);
      out.add(new EpubLayoutBlock(row.title(), row.depth(), List.copyOf(perBlock.get(i))));
    }
    return out;
  }

  private static void assertNavDepthWithinLimit(List<NavRow> navRows) {
    for (NavRow row : navRows) {
      if (row.depth() > BookReadingWireConstants.MAX_LAYOUT_DEPTH) {
        throw new ApiException(
            "EPUB table of contents exceeds maximum depth of "
                + BookReadingWireConstants.MAX_LAYOUT_DEPTH,
            ApiError.ErrorType.BINDING_ERROR,
            "EPUB table of contents exceeds maximum depth of "
                + BookReadingWireConstants.MAX_LAYOUT_DEPTH);
      }
    }
  }

  private static List<NavRow> navRowsFromNavDocument(
      byte[] epubBytes, OpfContext ctx, String navHref) {
    String navZipPath = resolveAgainstOpf(ctx.opfEntryName(), navHref);
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
    Element rootOl = firstChildElement(tocNav, "ol");
    if (rootOl == null) {
      return List.of();
    }
    List<NavRow> navRows = new ArrayList<>();
    walkOl(rootOl, 0, navZipPath, navRows);
    return navRows;
  }

  private static List<NavRow> navRowsFromHeadings(byte[] epubBytes, List<String> spineXhtmlPaths) {
    List<NavRow> out = new ArrayList<>();
    for (String spinePath : spineXhtmlPaths) {
      Element body = loadBody(epubBytes, spinePath);
      if (body == null) {
        continue;
      }
      IdentityHashMap<Element, Integer> preorder = new IdentityHashMap<>();
      int[] counter = {0};
      assignPreorder(body, counter, preorder);
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

  private static Element loadBody(byte[] epubBytes, String spineZipPath) {
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
      Document doc = parseXmlSecure(xhtmlBytes);
      return findBody(doc);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      return null;
    }
  }

  private static boolean isHeadingTag(String localName) {
    if (localName == null || localName.length() != 2 || localName.charAt(0) != 'h') {
      return false;
    }
    char c = localName.charAt(1);
    return c >= '1' && c <= '6';
  }

  private static int headingLevel(String localName) {
    return localName.charAt(1) - '0';
  }

  private static void walkElementForHeadings(
      Element el,
      String spineZipPath,
      List<Integer> levelStack,
      List<NavRow> out,
      IdentityHashMap<Element, Integer> preorder) {
    String local = el.getLocalName();
    if (isHeadingTag(local)) {
      int level = headingLevel(local);
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
        out.add(new NavRow(title.trim(), depth, spineZipPath, frag, startIfNoFrag));
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

  private static Map<String, Object> beginningAnchorPayload(Map<String, Object> firstOrphan) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", "beginning_anchor");
    m.put("kind", "beginning");
    m.put("href", firstOrphan.get("href"));
    m.put("fragment", firstOrphan.get("fragment"));
    return m;
  }

  private record OpfContext(String opfEntryName, Element packageEl) {}

  private static OpfContext loadOpfContext(byte[] epubBytes) {
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
    return new OpfContext(opfEntryName, packageEl);
  }

  private static List<String> spineXhtmlPaths(OpfContext ctx) {
    Element manifestEl = firstChildByLocal(ctx.packageEl(), "manifest");
    Element spineEl = firstChildByLocal(ctx.packageEl(), "spine");
    if (manifestEl == null || spineEl == null) {
      return List.of();
    }
    Map<String, ManifestItem> byId = manifestItems(ctx.opfEntryName(), manifestEl);
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
      if (item == null) {
        continue;
      }
      if (!isXhtmlMediaType(item.mediaType())) {
        continue;
      }
      paths.add(item.zipPath());
    }
    return paths;
  }

  private record ManifestItem(String zipPath, String mediaType) {}

  private static Map<String, ManifestItem> manifestItems(String opfEntryName, Element manifestEl) {
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
      String zipPath = resolveAgainstOpf(opfEntryName, href);
      String mt = item.getAttribute("media-type");
      map.put(id.trim(), new ManifestItem(zipPath, mt == null ? "" : mt.trim()));
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

  private static Element firstChildByLocal(Element parent, String localName) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n instanceof Element el && localName.equals(el.getLocalName())) {
        return el;
      }
    }
    return null;
  }

  private static void extractContentForSpineFile(
      byte[] epubBytes,
      String spineZipPath,
      List<NavRow> navRows,
      List<List<Map<String, Object>>> perBlock,
      List<Map<String, Object>> orphans) {
    List<Integer> targeting = new ArrayList<>();
    for (int i = 0; i < navRows.size(); i++) {
      if (spineZipPath.equals(navRows.get(i).spineZipPath())) {
        targeting.add(i);
      }
    }
    if (targeting.isEmpty()) {
      return;
    }
    byte[] xhtmlBytes;
    try {
      xhtmlBytes = readEntryBytes(epubBytes, spineZipPath, MAX_SPINE_XHTML_BYTES);
    } catch (IOException e) {
      throw readError();
    }
    if (xhtmlBytes == null) {
      return;
    }
    Document doc;
    try {
      doc = parseXmlSecure(xhtmlBytes);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      return;
    }
    Element body = findBody(doc);
    if (body == null) {
      return;
    }
    IdentityHashMap<Element, Integer> preorder = new IdentityHashMap<>();
    int[] counter = {0};
    assignPreorder(body, counter, preorder);

    List<SectionStart> sections = new ArrayList<>();
    for (int navIdx : targeting) {
      NavRow row = navRows.get(navIdx);
      String frag = row.fragmentId();
      if (frag == null || frag.isEmpty()) {
        Integer start = row.startPreorderIfNoFragment();
        sections.add(new SectionStart(navIdx, start != null ? start : DOC_START_PREORDER));
      } else {
        Element targetEl = findElementByIdAttr(doc, frag);
        if (targetEl == null) {
          sections.add(new SectionStart(navIdx, DOC_START_PREORDER));
        } else {
          Integer po = preorder.get(targetEl);
          sections.add(new SectionStart(navIdx, po != null ? po : DOC_START_PREORDER));
        }
      }
    }
    sections.sort(
        Comparator.comparingInt(SectionStart::startPreorder)
            .thenComparingInt(SectionStart::navIndex));

    emitContentUnderBody(body, spineZipPath, preorder, sections, perBlock, orphans);

    for (int navIdx : targeting) {
      String fragId = navRows.get(navIdx).fragmentId();
      if (fragId == null || fragId.isBlank()) {
        continue;
      }
      List<Map<String, Object>> payloads = perBlock.get(navIdx);
      if (payloads.isEmpty()) {
        continue;
      }
      payloads.getFirst().put("fragment", fragId.trim());
    }
  }

  /**
   * XHTML in EPUB often has no DTD declaring {@code id} as type ID, so {@link
   * Document#getElementById} may return null; fall back to scanning {@code id} attributes.
   */
  private static Element findElementByIdAttr(Document doc, String id) {
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

  private static void emitContentUnderBody(
      Element body,
      String spineZipPath,
      IdentityHashMap<Element, Integer> preorder,
      List<SectionStart> sections,
      List<List<Map<String, Object>>> perBlock,
      List<Map<String, Object>> orphans) {
    NodeList children = body.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n instanceof Element child) {
        emitFromElement(child, spineZipPath, preorder, sections, perBlock, orphans);
      }
    }
  }

  private static void emitFromElement(
      Element el,
      String spineZipPath,
      IdentityHashMap<Element, Integer> preorder,
      List<SectionStart> sections,
      List<List<Map<String, Object>>> perBlock,
      List<Map<String, Object>> orphans) {
    String local = el.getLocalName();
    if ("p".equals(local)) {
      emitParagraphIfNonEmpty(el, spineZipPath, preorder, sections, perBlock, orphans);
      return;
    }
    if ("img".equals(local)) {
      emitImage(el, spineZipPath, preorder, sections, perBlock, orphans);
      return;
    }
    if ("table".equals(local)) {
      emitTable(el, spineZipPath, preorder, sections, perBlock, orphans);
      return;
    }
    NodeList children = el.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n instanceof Element child) {
        emitFromElement(child, spineZipPath, preorder, sections, perBlock, orphans);
      }
    }
  }

  private static void emitParagraphIfNonEmpty(
      Element p,
      String spineZipPath,
      IdentityHashMap<Element, Integer> preorder,
      List<SectionStart> sections,
      List<List<Map<String, Object>>> perBlock,
      List<Map<String, Object>> orphans) {
    String text = p.getTextContent() != null ? p.getTextContent().trim() : "";
    if (text.isEmpty()) {
      NodeList children = p.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node n = children.item(i);
        if (n instanceof Element child) {
          emitFromElement(child, spineZipPath, preorder, sections, perBlock, orphans);
        }
      }
      return;
    }
    Integer po = preorder.get(p);
    if (po == null) {
      return;
    }
    int owner = ownerNavIndex(po, sections);
    if (owner < 0) {
      orphans.add(contentMap("text", spineZipPath, fragmentFor(p), text, null, null));
      return;
    }
    perBlock.get(owner).add(contentMap("text", spineZipPath, fragmentFor(p), text, null, null));
    NodeList children = p.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n instanceof Element child) {
        emitFromElement(child, spineZipPath, preorder, sections, perBlock, orphans);
      }
    }
  }

  private static void emitImage(
      Element img,
      String spineZipPath,
      IdentityHashMap<Element, Integer> preorder,
      List<SectionStart> sections,
      List<List<Map<String, Object>>> perBlock,
      List<Map<String, Object>> orphans) {
    Integer po = preorder.get(img);
    if (po == null) {
      return;
    }
    int owner = ownerNavIndex(po, sections);
    if (owner < 0) {
      String srcOrphan = img.getAttribute("src");
      if (srcOrphan == null) {
        srcOrphan = "";
      }
      orphans.add(
          contentMap("image", spineZipPath, fragmentFor(img), null, srcOrphan.trim(), null));
      return;
    }
    String src = img.getAttribute("src");
    if (src == null) {
      src = "";
    }
    perBlock
        .get(owner)
        .add(contentMap("image", spineZipPath, fragmentFor(img), null, src.trim(), null));
  }

  private static void emitTable(
      Element table,
      String spineZipPath,
      IdentityHashMap<Element, Integer> preorder,
      List<SectionStart> sections,
      List<List<Map<String, Object>>> perBlock,
      List<Map<String, Object>> orphans) {
    Integer po = preorder.get(table);
    if (po == null) {
      return;
    }
    int owner = ownerNavIndex(po, sections);
    String text = table.getTextContent() != null ? table.getTextContent().trim() : "";
    if (owner < 0) {
      orphans.add(contentMap("table", spineZipPath, fragmentFor(table), null, null, text));
      return;
    }
    perBlock
        .get(owner)
        .add(contentMap("table", spineZipPath, fragmentFor(table), null, null, text));
  }

  private static int ownerNavIndex(int contentPreorder, List<SectionStart> sortedSections) {
    if (sortedSections.isEmpty()) {
      return -1;
    }
    SectionStart first = sortedSections.getFirst();
    if (first.startPreorder() > DOC_START_PREORDER && contentPreorder < first.startPreorder()) {
      return -1;
    }
    int owner = first.navIndex();
    for (SectionStart s : sortedSections) {
      if (s.startPreorder() <= contentPreorder) {
        owner = s.navIndex();
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

  private static Map<String, Object> contentMap(
      String type, String href, String fragment, String text, String src, String tableText) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", type);
    m.put("href", href);
    m.put("fragment", fragment);
    if (text != null) {
      m.put("text", text);
    }
    if (src != null) {
      m.put("src", src);
    }
    if (tableText != null) {
      m.put("text", tableText);
    }
    return m;
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

  /** Resolves a TOC {@code href} to a ZIP path; fragment (if any) is ignored. */
  private static String resolveAgainstNavDoc(String navZipPath, String hrefTrimmed) {
    String href = hrefTrimmed.trim();
    int hash = href.indexOf('#');
    String pathPart = hash >= 0 ? href.substring(0, hash) : href;
    if (pathPart.contains("..")) {
      throw new ApiException(
          "EPUB navigation href is not allowed",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB navigation href is not allowed");
    }
    Path navPath = Paths.get(navZipPath.replace('\\', '/'));
    Path navParent = navPath.getParent() != null ? navPath.getParent() : Paths.get("");
    if (pathPart.isEmpty()) {
      return navZipPath.replace('\\', '/');
    }
    Path resolved = navParent.resolve(pathPart).normalize();
    if (resolved.isAbsolute()) {
      throw new ApiException(
          "EPUB navigation href is not allowed",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB navigation href is not allowed");
    }
    return resolved.toString().replace('\\', '/');
  }

  private static NavRow navRowFromAnchor(Element anchor, int depth, String navZipPath) {
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
    return new NavRow(title.trim(), depth, spineZip, frag, null);
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

  private static void walkOl(Element ol, int itemDepth, String navZipPath, List<NavRow> out) {
    NodeList children = ol.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (!(n instanceof Element li) || !"li".equals(li.getLocalName())) {
        continue;
      }
      Element anchor = firstAnchorInLi(li);
      if (anchor != null) {
        NavRow row = navRowFromAnchor(anchor, itemDepth, navZipPath);
        if (row != null) {
          out.add(row);
        }
      }
      Element nestedOl = firstDirectChildOl(li);
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

  private static ApiException readError() {
    return new ApiException(
        "EPUB file could not be read",
        ApiError.ErrorType.BINDING_ERROR,
        "EPUB file could not be read");
  }
}
