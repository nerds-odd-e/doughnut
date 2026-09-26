package com.odde.donut.configs;

import com.google.cloud.storage.Storage;
import com.odde.donut.entities.repositories.AttachmentBlobRepository;
import com.odde.donut.services.book.BookStorage;
import com.odde.donut.services.book.DbBookStorage;
import com.odde.donut.services.book.GcsBookStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class BookStorageConfiguration {

  @Bean
  @Profile("prod")
  BookStorage gcsBookStorage(
      Storage storage,
      @Value("${donut.book-pdf.gcs.bucket}") String bucket,
      @Value("${donut.book-pdf.gcs.object-prefix:}") String objectPrefix) {
    return new GcsBookStorage(storage, bucket, objectPrefix);
  }

  @Bean
  @Profile("!prod")
  BookStorage dbBookStorage(AttachmentBlobRepository attachmentBlobRepository) {
    return new DbBookStorage(attachmentBlobRepository);
  }
}
