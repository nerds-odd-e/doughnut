package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.testability.MakeMe;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotebookExportServiceTest {
  @Autowired NotebookExportService notebookExportService;
  @Autowired MakeMe makeMe;

  private Map<String, String> readZipEntries(byte[] zipBytes) throws IOException {
    Map<String, String> entries = new LinkedHashMap<>();
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        entries.put(entry.getName(), new String(zis.readAllBytes(), StandardCharsets.UTF_8));
      }
    }
    return entries;
  }

  @Test
  void exportsNotesInsideFoldersAsMarkdownFiles() throws IOException {
    Folder folder =
        makeMe.aFolder().notebookOwnedBy(makeMe.aUser().please()).name("Recipes").please();
    makeMe.aNote("Pasta").folder(folder).content("Boil water").please();
    makeMe.entityPersister.flush();

    Map<String, String> entries =
        readZipEntries(notebookExportService.exportNotebookAsZip(folder.getNotebook()));

    assertThat(entries.get("Recipes/Pasta.md"), equalTo("Boil water"));
  }

  @Test
  void exportFileNameIsNotebookNameZip() {
    Notebook notebook = makeMe.aNotebook().name("Q&A Notes").please();

    assertThat(notebookExportService.exportFileName(notebook), equalTo("Q&A Notes.zip"));
  }
}
