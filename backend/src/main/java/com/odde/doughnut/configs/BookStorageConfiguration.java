package com.odde.doughnut.configs;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.odde.doughnut.entities.repositories.AttachmentBlobRepository;
import com.odde.doughnut.services.book.BookStorage;
import com.odde.doughnut.services.book.DbBookStorage;
import com.odde.doughnut.services.book.GcsBookStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class BookStorageConfiguration {

  @Bean
  @Profile("prod")
  Storage bookGcsClient() {
    return StorageOptions.getDefaultInstance().getService();
  }

  @Bean
  @Profile("prod")
  BookStorage gcsBookStorage(
      Storage bookGcsClient,
      @Value("${doughnut.book-pdf.gcs.bucket}") String bucket,
      @Value("${doughnut.book-pdf.gcs.object-prefix:}") String objectPrefix) {
    return new GcsBookStorage(bookGcsClient, bucket, objectPrefix);
  }

  @Bean
  @Profile("!prod")
  BookStorage dbBookStorage(AttachmentBlobRepository attachmentBlobRepository) {
    return new DbBookStorage(attachmentBlobRepository);
  }
}
