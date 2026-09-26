package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import com.odde.donut.services.notebookAttachment.GcsNotebookAttachmentContent;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = {NotebookAttachmentContentConfiguration.class},
    properties = {"donut.notebook-attachment.gcs.bucket=test-bucket"})
@ActiveProfiles("prod")
class NotebookAttachmentContentConfigurationProdTest {

  @Autowired NotebookAttachmentContent notebookAttachmentContent;

  @Test
  void usesGcsNotebookAttachmentContentUnderProdProfile() {
    assertThat(notebookAttachmentContent, instanceOf(GcsNotebookAttachmentContent.class));
  }
}
