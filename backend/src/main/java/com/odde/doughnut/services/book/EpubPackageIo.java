package com.odde.doughnut.services.book;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.exceptions.ApiException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Shared ZIP entry reads and secure XML parsing for EPUB validation and structure extraction. */
final class EpubPackageIo {

  static final String NS_CONTAINER = "urn:oasis:names:tc:opendocument:xmlns:container";
  static final String NS_OPF = "http://www.idpf.org/2007/opf";

  static final int MAX_CONTAINER_BYTES = 256 * 1024;
  static final int MAX_OPF_BYTES = 5 * 1024 * 1024;
  static final int MAX_NAV_BYTES = 5 * 1024 * 1024;

  /** Upper bound for each spine XHTML document read during structure extraction. */
  static final int MAX_SPINE_XHTML_BYTES = MAX_NAV_BYTES;

  private EpubPackageIo() {}

  static byte[] readEntryBytes(byte[] epubZip, String entryPath, int maxBytes) throws IOException {
    String target = normalizeEntryName(entryPath);
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(epubZip))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          zis.closeEntry();
          continue;
        }
        String name = normalizeEntryName(entry.getName());
        if (target.equals(name)) {
          byte[] content = zis.readNBytes(maxBytes + 1);
          zis.closeEntry();
          if (content.length > maxBytes) {
            throw new ApiException(
                "EPUB metadata file is too large",
                ApiError.ErrorType.BINDING_ERROR,
                "EPUB metadata file is too large");
          }
          return content;
        }
        drain(zis);
        zis.closeEntry();
      }
    }
    return null;
  }

  static void drain(InputStream in) throws IOException {
    in.transferTo(OutputStream.nullOutputStream());
  }

  static String normalizeEntryName(String raw) {
    if (raw == null) {
      return "";
    }
    return raw.replace('\\', '/');
  }

  static String parseContainerRootfileFullPath(byte[] containerXml) {
    try {
      Document doc = parseXmlSecure(containerXml);
      Element root = doc.getDocumentElement();
      if (root == null) {
        return null;
      }
      NodeList rootfiles = root.getElementsByTagNameNS(NS_CONTAINER, "rootfile");
      if (rootfiles.getLength() == 0) {
        rootfiles = doc.getElementsByTagName("rootfile");
      }
      for (int i = 0; i < rootfiles.getLength(); i++) {
        Node n = rootfiles.item(i);
        if (n instanceof Element el) {
          String fullPath = el.getAttribute("full-path");
          if (fullPath != null && !fullPath.isBlank()) {
            return fullPath.trim();
          }
        }
      }
      return null;
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new ApiException(
          "EPUB META-INF/container.xml is invalid or unreadable",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB META-INF/container.xml is invalid or unreadable");
    }
  }

  static Document parseXmlSecure(byte[] xml)
      throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
    dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    dbf.setXIncludeAware(false);
    dbf.setExpandEntityReferences(false);
    return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
  }
}
