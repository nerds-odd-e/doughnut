package com.odde.doughnut.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.main.lazy-initialization=true",
      "spring.jmx.enabled=false",
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTests {
  private static final String TRUE_COPY = loadTrueCopy();

  @Autowired private MockMvc mockMvc;

  @Test
  void openApiDocsApprovalTest() throws Exception {
    String currentApiDocs =
        mockMvc.perform(get("/api-docs.yaml")).andReturn().getResponse().getContentAsString();

    assertEquals(
        TRUE_COPY,
        currentApiDocs,
        () ->
            "OpenAPI docs differ from open_api_docs.yaml. Run `pnpm generateTypeScript` to sync.");
  }

  private static String loadTrueCopy() {
    try {
      return Files.readString(Path.of("../open_api_docs.yaml"));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read open_api_docs.yaml", e);
    }
  }
}
