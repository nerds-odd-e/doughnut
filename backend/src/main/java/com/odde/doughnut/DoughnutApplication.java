package com.odde.doughnut;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DoughnutApplication {
  public static void main(String[] args) {
    SpringApplication.run(DoughnutApplication.class, args);
  }
}

