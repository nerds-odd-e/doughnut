package com.odde.doughnut.integration;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RobotsTests {
  private static final String EXPECTED_OPEN_API_DOCS = readExpectedOpenApiDocsYaml();

  @Autowired private MockMvc mockMvc;

  @Test
  @Order(1)
  void robotsTxt() throws Exception {
    mockMvc
        .perform(get("/robots.txt"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("User-agent: *")));
  }

  @Test
  @Order(2)
  void openApiDocsMatchCommittedYaml() throws Exception {
    String currentApiDocs =
        mockMvc.perform(get("/api-docs.yaml")).andReturn().getResponse().getContentAsString();

    assertEquals(
        EXPECTED_OPEN_API_DOCS,
        currentApiDocs,
        () ->
            "OpenAPI docs differ from open_api_docs.yaml. Run `pnpm generateTypeScript` to sync.");
  }

  private static String readExpectedOpenApiDocsYaml() {
    try {
      return Files.readString(Path.of("../open_api_docs.yaml"));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read open_api_docs.yaml", e);
    }
  }
}
