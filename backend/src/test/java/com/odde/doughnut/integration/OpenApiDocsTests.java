package com.odde.doughnut.integration;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

    if (!trueCopy.equals(currentApiDocs)) {
      String diffMessage = generateDiffMessage(trueCopy, currentApiDocs);
      fail(diffMessage);
    }
  }

  private String generateDiffMessage(String expected, String actual) {
    String[] expectedLines = expected.split("\n", -1);
    String[] actualLines = actual.split("\n", -1);

    int maxLines = Math.max(expectedLines.length, actualLines.length);
    int firstDiffLine = -1;
    int lastDiffLine = -1;
    int diffCount = 0;

    for (int i = 0; i < maxLines; i++) {
      String expectedLine = i < expectedLines.length ? expectedLines[i] : "";
      String actualLine = i < actualLines.length ? actualLines[i] : "";
      if (!expectedLine.equals(actualLine)) {
        diffCount++;
        if (firstDiffLine == -1) {
          firstDiffLine = i + 1;
        }
        lastDiffLine = i + 1;
      }
    }

    StringBuilder message = new StringBuilder();
    message.append("OpenAPI docs differ: ")
        .append(diffCount)
        .append(" line(s) different");

    if (firstDiffLine != -1) {
      message.append(" (lines ").append(firstDiffLine);
      if (lastDiffLine != firstDiffLine) {
        message.append("-").append(lastDiffLine);
      }
      message.append(")");
    }

    message.append("\n\nRun `pnpm generateTypeScript` to sync the docs.\n\n");

    // Generate truncated diff (max 100 lines)
    List<String> diffLines = new ArrayList<>();
    int contextLines = 2;
    int maxDiffLines = 100;
    int startLine = Math.max(0, firstDiffLine - 1 - contextLines);
    int endLine = Math.min(maxLines, lastDiffLine + contextLines);

    for (int i = startLine; i < endLine && diffLines.size() < maxDiffLines; i++) {
      String expectedLine = i < expectedLines.length ? expectedLines[i] : "";
      String actualLine = i < actualLines.length ? actualLines[i] : "";
      boolean isDifferent = !expectedLine.equals(actualLine);

      if (isDifferent) {
        diffLines.add(String.format("%4d | -%s", i + 1, expectedLine));
        if (diffLines.size() < maxDiffLines) {
          diffLines.add(String.format("%4d | +%s", i + 1, actualLine));
        }
      } else if (i >= firstDiffLine - 1 - contextLines && i <= lastDiffLine + contextLines) {
        // Show context lines around differences
        if (diffLines.size() < maxDiffLines) {
          diffLines.add(String.format("%4d |  %s", i + 1, actualLine));
        }
      }
    }

    if (diffLines.size() > 0) {
      message.append("Diff (truncated):\n");
      for (String diffLine : diffLines) {
        message.append(diffLine).append("\n");
      }
    }

    return message.toString();
  }
}
