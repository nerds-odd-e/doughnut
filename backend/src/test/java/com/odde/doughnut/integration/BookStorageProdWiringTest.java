package com.odde.doughnut.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.google.cloud.storage.Storage;
import com.odde.doughnut.configs.BookStorageConfiguration;
import com.odde.doughnut.entities.repositories.BookRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.GithubService;
import com.odde.doughnut.services.book.BookService;
import com.odde.doughnut.services.book.BookStorage;
import com.odde.doughnut.services.book.GcsBookStorage;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;

class BookStorageProdWiringTest {

  @Configuration
  @Import({BookStorageConfiguration.class, Collaborators.class, StorageOverride.class})
  static class App {}

  @Configuration
  static class StorageOverride {
    @Bean
    Storage bookGcsClient() {
      return mock(Storage.class);
    }
  }

  @Configuration
  static class Collaborators {
    @Bean
    BookRepository bookRepository() {
      return mock(BookRepository.class);
    }

    @Bean
    EntityPersister entityPersister() {
      return mock(EntityPersister.class);
    }

    @Bean
    GithubService githubService() {
      return mock(GithubService.class);
    }

    @Bean
    TestabilitySettings testabilitySettings() {
      return new TestabilitySettings();
    }

    @Bean
    BookService bookService(
        BookRepository bookRepository,
        EntityPersister entityPersister,
        TestabilitySettings testabilitySettings,
        BookStorage bookStorage) {
      return new BookService(bookRepository, entityPersister, testabilitySettings, bookStorage);
    }
  }

  @Test
  void prodProfileWiresSingleGcsBookStorageIntoBookService() {
    try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
      ctx.getEnvironment().setActiveProfiles("prod");
      ctx.getEnvironment()
          .getPropertySources()
          .addFirst(
              new MapPropertySource(
                  "bookStorageProdWiringTest",
                  Map.of(
                      "doughnut.book-pdf.gcs.bucket",
                      "test-book-pdf-bucket",
                      "doughnut.book-pdf.gcs.object-prefix",
                      "test-prefix/",
                      "spring.main.allow-bean-definition-overriding",
                      "true")));
      ctx.setAllowBeanDefinitionOverriding(true);
      ctx.register(App.class);
      ctx.refresh();

      BookStorage bookStorage = ctx.getBean(BookStorage.class);
      BookService bookService = ctx.getBean(BookService.class);

      assertEquals(1, ctx.getBeanNamesForType(BookStorage.class).length);
      assertInstanceOf(GcsBookStorage.class, bookStorage);
      assertNotNull(bookService);
    }
  }
}
