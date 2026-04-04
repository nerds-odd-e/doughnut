package com.odde.doughnut.configs;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.odde.doughnut.entities.repositories.AttachmentBlobRepository;
import com.odde.doughnut.services.book.BookPdfStorage;
import com.odde.doughnut.services.book.DbBookPdfStorage;
import com.odde.doughnut.services.book.GcsBookPdfStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class BookPdfStorageConfiguration {

  @Bean
  @Profile("prod")
  Storage bookPdfGcsClient() {
    return StorageOptions.getDefaultInstance().getService();
  }

  @Bean
  @Profile("prod")
  BookPdfStorage gcsBookPdfStorage(
      Storage bookPdfGcsClient,
      @Value("${doughnut.book-pdf.gcs.bucket}") String bucket,
      @Value("${doughnut.book-pdf.gcs.object-prefix:}") String objectPrefix) {
    return new GcsBookPdfStorage(bookPdfGcsClient, bucket, objectPrefix);
  }

  @Bean
  @Profile("!prod")
  BookPdfStorage dbBookPdfStorage(AttachmentBlobRepository attachmentBlobRepository) {
    return new DbBookPdfStorage(attachmentBlobRepository);
  }
}
