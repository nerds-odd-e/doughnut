package com.odde.donut.configs;

import com.google.cloud.storage.Storage;
import com.odde.donut.services.notebookAttachment.GcsNotebookAttachmentContent;
import com.odde.donut.services.notebookAttachment.InMemoryNotebookAttachmentContent;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class NotebookAttachmentContentConfiguration {

  @Bean
  @Profile("prod")
  NotebookAttachmentContent gcsNotebookAttachmentContent(
      Storage storage,
      @Value("${donut.notebook-attachment.gcs.bucket}") String bucket,
      @Value("${donut.notebook-attachment.gcs.object-prefix:}") String objectPrefix) {
    return new GcsNotebookAttachmentContent(storage, bucket, objectPrefix);
  }

  @Bean
  @Profile("!prod")
  NotebookAttachmentContent inMemoryNotebookAttachmentContent() {
    return new InMemoryNotebookAttachmentContent();
  }
}
