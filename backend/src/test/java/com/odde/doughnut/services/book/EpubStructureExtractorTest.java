package com.odde.doughnut.services.book;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EpubStructureExtractorTest {

  private static final String CONTAINER_XML =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
        <rootfiles>
          <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
        </rootfiles>
      </container>
      """;

  private static final String MINIMAL_OPF_NO_NAV =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid">
        <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
          <dc:identifier id="uid">test-id</dc:identifier>
          <dc:title>Test</dc:title>
          <dc:language>en</dc:language>
        </metadata>
        <manifest>
          <item id="ch1" href="chapter.xhtml" media-type="application/xhtml+xml"/>
        </manifest>
        <spine>
          <itemref idref="ch1"/>
        </spine>
      </package>
      """;

  @Test
  void headingFallbackBuildsBlocksFromSpineHeadings() throws Exception {
    String xhtml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <html xmlns="http://www.w3.org/1999/xhtml">
        <head><title>t</title></head>
        <body>
          <h1>Intro</h1>
          <p>Body one.</p>
          <h2>Section</h2>
          <p>Body two.</p>
        </body>
        </html>
        """;
    byte[] epub = buildMinimalEpub(xhtml.getBytes(StandardCharsets.UTF_8));

    Assertions.assertDoesNotThrow(() -> EpubAttachValidator.validateAttachableEpub(epub));

    List<EpubStructureExtractor.EpubLayoutBlock> layout =
        EpubStructureExtractor.extractEpubLayoutWithContent(epub);

    assertThat(layout, hasSize(2));
    assertThat(layout.get(0).title(), equalTo("Intro"));
    assertThat(layout.get(0).depth(), equalTo(0));
    assertThat(layout.get(0).contentPayloads(), hasSize(1));
    assertThat(layout.get(0).contentPayloads().getFirst().get("text"), equalTo("Body one."));
    assertThat(layout.get(1).title(), equalTo("Section"));
    assertThat(layout.get(1).depth(), equalTo(1));
    assertThat(layout.get(1).contentPayloads().getFirst().get("text"), equalTo("Body two."));
  }

  @Test
  void leadingBodyContentBeforeFirstHeadingYieldsBeginningBlock() throws Exception {
    String xhtml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <html xmlns="http://www.w3.org/1999/xhtml">
        <head><title>t</title></head>
        <body>
          <p id="lead">Lead text.</p>
          <h1 id="main">Main</h1>
          <p>After.</p>
        </body>
        </html>
        """;
    byte[] epub = buildMinimalEpub(xhtml.getBytes(StandardCharsets.UTF_8));

    Assertions.assertDoesNotThrow(() -> EpubAttachValidator.validateAttachableEpub(epub));

    List<EpubStructureExtractor.EpubLayoutBlock> layout =
        EpubStructureExtractor.extractEpubLayoutWithContent(epub);

    assertThat(layout, hasSize(2));
    EpubStructureExtractor.EpubLayoutBlock beginning = layout.getFirst();
    assertThat(beginning.title(), equalTo("*beginning*"));
    assertThat(beginning.depth(), equalTo(0));
    assertThat(beginning.contentPayloads(), hasSize(2));
    Map<String, Object> anchor = beginning.contentPayloads().getFirst();
    assertThat(anchor.get("type"), equalTo("beginning_anchor"));
    assertThat(anchor.get("kind"), equalTo("beginning"));
    assertThat(anchor.get("href"), equalTo("OEBPS/chapter.xhtml"));
    assertThat(anchor.get("fragment"), equalTo("lead"));
    assertThat(beginning.contentPayloads().get(1).get("text"), equalTo("Lead text."));

    EpubStructureExtractor.EpubLayoutBlock main = layout.get(1);
    assertThat(main.title(), equalTo("Main"));
    assertThat(main.contentPayloads(), hasSize(1));
    assertThat(main.contentPayloads().getFirst().get("text"), equalTo("After."));
  }

  private static byte[] buildMinimalEpub(byte[] chapterBytes) throws IOException {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    entries.put("META-INF/container.xml", CONTAINER_XML.getBytes(StandardCharsets.UTF_8));
    entries.put("OEBPS/content.opf", MINIMAL_OPF_NO_NAV.getBytes(StandardCharsets.UTF_8));
    entries.put("OEBPS/chapter.xhtml", chapterBytes);
    return zipEntries(entries);
  }

  private static byte[] zipEntries(Map<String, byte[]> pathToContent) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
      for (Map.Entry<String, byte[]> e : pathToContent.entrySet()) {
        zos.putNextEntry(new java.util.zip.ZipEntry(e.getKey()));
        zos.write(e.getValue());
        zos.closeEntry();
      }
    }
    return baos.toByteArray();
  }
}
