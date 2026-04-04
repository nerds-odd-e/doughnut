package com.odde.doughnut.configs;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.odde.doughnut.entities.repositories.AttachmentBlobRepository;
import com.odde.doughnut.services.book.BookPdfStorage;
import com.odde.doughnut.services.book.DbBookPdfStorage;
import com.odde.doughnut.services.book.GcsBookPdfStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookPdfStorageConfiguration {

  @Bean
  @ConditionalOnProperty(prefix = "doughnut.book-pdf.gcs", name = "bucket")
  Storage bookPdfGcsClient() {
    return StorageOptions.getDefaultInstance().getService();
  }

  @Bean
  @ConditionalOnProperty(prefix = "doughnut.book-pdf.gcs", name = "bucket")
  BookPdfStorage gcsBookPdfStorage(
      Storage bookPdfGcsClient,
      @Value("${doughnut.book-pdf.gcs.bucket}") String bucket,
      @Value("${doughnut.book-pdf.gcs.object-prefix:}") String objectPrefix) {
    return new GcsBookPdfStorage(bookPdfGcsClient, bucket, objectPrefix);
  }

  @Bean
  @ConditionalOnMissingBean(BookPdfStorage.class)
  BookPdfStorage dbBookPdfStorage(AttachmentBlobRepository attachmentBlobRepository) {
    return new DbBookPdfStorage(attachmentBlobRepository);
  }
}
