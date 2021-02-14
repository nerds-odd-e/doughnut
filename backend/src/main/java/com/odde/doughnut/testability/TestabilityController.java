
package com.odde.doughnut.testability;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManagerFactory;

@RestController
@RequestMapping("/api/testability")
class TestabilityController {
  @Autowired
  EntityManagerFactory emf;

  @GetMapping("/clean_db")
  public String cleanDB() {
    new DBCleanerWorker(emf).truncateAllTables();
    return "OK";
  }
}
