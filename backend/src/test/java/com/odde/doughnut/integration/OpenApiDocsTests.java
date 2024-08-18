package com.odde.doughnut.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTests {
  @Autowired private MockMvc mockMvc;

  @Test
  void openApiDocsApprovalTest() throws Exception {
    //
    // This test compares the current OpenAPI documentation with the approved 'true copy'.
    // If the current documentation does not match the 'true copy', the test will fail.
    // If the changes are intended, run command:
    //    pnpm generateTypeScript
    // to regenerate the 'true copy' openAPI docs and then the TypeScript code based on it.
    //
    // In case the generated OpenAPI documentation contains entity fields in random order,
    // use @JsonPropertyOrder annotation in the entity class to specify the order of the fields.
    //

    ResultActions perform = this.mockMvc.perform(get("/api-docs.yaml"));
    String currentApiDocs = perform.andReturn().getResponse().getContentAsString();

    Path tempFile = Files.createTempFile("api-docs-current", ".yaml");
    String trueCopyPath = "../open_api_docs.yaml";

    Files.writeString(tempFile, currentApiDocs);
    String trueCopy = new String(Files.readAllBytes(Paths.get(trueCopyPath)));

    assertEquals(
        trueCopy,
        currentApiDocs,
        "The current OpenAPI documentation does not match the approved 'true copy'. "
            + "Please review the changes in '%s'. If the changes are intended, ".formatted(tempFile)
            + "copy it over '%s' and rerun the test.".formatted(trueCopyPath));
  }
}
