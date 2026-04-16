package com.odde.doughnut.services.book;

import static com.odde.doughnut.services.book.EpubPackageIo.MAX_CONTAINER_BYTES;
import static com.odde.doughnut.services.book.EpubPackageIo.MAX_OPF_BYTES;
import static com.odde.doughnut.services.book.EpubPackageIo.NS_OPF;
import static com.odde.doughnut.services.book.EpubPackageIo.drain;
import static com.odde.doughnut.services.book.EpubPackageIo.normalizeEntryName;
import static com.odde.doughnut.services.book.EpubPackageIo.parseContainerRootfileFullPath;
import static com.odde.doughnut.services.book.EpubPackageIo.parseXmlSecure;
import static com.odde.doughnut.services.book.EpubPackageIo.readEntryBytes;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.exceptions.ApiException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

final class EpubAttachValidator {

  private EpubAttachValidator() {}

  static void validateAttachableEpub(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      throw new ApiException(
          "EPUB file is empty", ApiError.ErrorType.BINDING_ERROR, "EPUB file is empty");
    }
    boolean hasEncryption;
    byte[] containerBytes;
    try {
      hasEncryption = scanEncryption(bytes);
      containerBytes = readEntryBytes(bytes, "META-INF/container.xml", MAX_CONTAINER_BYTES);
    } catch (ZipException e) {
      throw new ApiException(
          "EPUB file is not a valid ZIP archive",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB file is not a valid ZIP archive");
    } catch (IOException e) {
      throw new ApiException(
          "EPUB file could not be read",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB file could not be read");
    }
    if (hasEncryption) {
      throw new ApiException(
          "EPUB is encrypted or DRM-protected; only non-DRM EPUB is supported",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB is encrypted or DRM-protected; only non-DRM EPUB is supported");
    }
    if (containerBytes == null) {
      throw new ApiException(
          "EPUB is missing META-INF/container.xml",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB is missing META-INF/container.xml");
    }
    String opfPath = parseContainerRootfileFullPath(containerBytes);
    if (opfPath == null || opfPath.isBlank()) {
      throw new ApiException(
          "EPUB container.xml does not declare a package document",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB container.xml does not declare a package document");
    }
    String opfSlashes = opfPath.replace('\\', '/');
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
      opfBytes = readEntryBytes(bytes, opfEntryName, MAX_OPF_BYTES);
    } catch (ZipException e) {
      throw new ApiException(
          "EPUB file is not a valid ZIP archive",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB file is not a valid ZIP archive");
    } catch (IOException e) {
      throw new ApiException(
          "EPUB file could not be read",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB file could not be read");
    }
    if (opfBytes == null) {
      throw new ApiException(
          "EPUB package document is missing from the archive",
          ApiError.ErrorType.BINDING_ERROR,
          "EPUB package document is missing from the archive");
    }
    validateOpfPackage(opfBytes);
  }

  private static boolean scanEncryption(byte[] bytes) throws IOException {
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          zis.closeEntry();
          continue;
        }
        String name = normalizeEntryName(entry.getName());
        if ("META-INF/encryption.xml".equals(name)) {
          drain(zis);
          zis.closeEntry();
          return true;
        }
        drain(zis);
        zis.closeEntry();
      }
    }
    return false;
  }

  private static void validateOpfPackage(byte[] opfBytes) {
    try {
      Document doc = parseXmlSecure(opfBytes);
      Element root = doc.getDocumentElement();
      if (root == null) {
        throw invalidOpf();
      }
      String local = root.getLocalName();
      if (!"package".equals(local)) {
        throw invalidOpf();
      }
      String ns = root.getNamespaceURI();
      if (ns != null && !NS_OPF.equals(ns)) {
        throw invalidOpf();
      }
      if (!hasChildElement(doc, root, NS_OPF, "manifest")
          || !hasChildElement(doc, root, NS_OPF, "spine")) {
        throw new ApiException(
            "EPUB package document is missing manifest or spine",
            ApiError.ErrorType.BINDING_ERROR,
            "EPUB package document is missing manifest or spine");
      }
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw invalidOpf();
    }
  }

  private static boolean hasChildElement(Document doc, Element root, String ns, String local) {
    NodeList byNs = root.getElementsByTagNameNS(ns, local);
    if (byNs.getLength() > 0) {
      return true;
    }
    NodeList all = root.getChildNodes();
    for (int i = 0; i < all.getLength(); i++) {
      Node n = all.item(i);
      if (n instanceof Element el && local.equals(el.getLocalName())) {
        return true;
      }
    }
    return doc.getElementsByTagName(local).getLength() > 0;
  }

  private static ApiException invalidOpf() {
    return new ApiException(
        "EPUB package document is invalid or unreadable",
        ApiError.ErrorType.BINDING_ERROR,
        "EPUB package document is invalid or unreadable");
  }
}
