package com.odde.doughnut.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.file.Files;
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
    // If the changes are intended, copy the current documentation over the 'true copy' and rerun
    // the test.
    //
    // In case the generated OpenAPI documentation contains entity fields in random order,
    // use @JsonPropertyOrder annotation in the entity class to specify the order of the fields.
    //

    ResultActions perform = this.mockMvc.perform(get("/api-docs.yaml"));
    String currentApiDocs = perform.andReturn().getResponse().getContentAsString();

    String currentDocsPath = "src/test/resources/api-docs-current.yaml";
    String trueCopyPath = "../open_api_docs.yaml";

    Files.writeString(Paths.get(currentDocsPath), currentApiDocs);
    String trueCopy = new String(Files.readAllBytes(Paths.get(trueCopyPath)));

    assertThat(currentApiDocs)
        .as(
            "The current OpenAPI documentation does not match the approved 'true copy'. "
                + "Please review the changes in '%s'. If the changes are intended, "
                    .formatted(currentDocsPath)
                + "copy it over '%s' and rerun the test.".formatted(trueCopyPath))
        .isEqualTo(trueCopy);
  }
}
