package com.odde.doughnut;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.function.Consumer;

public class WithRepositories implements BeforeAllCallback {
    public void executeDB(Consumer<EntityManager> consumer) {
        String path = "/src/test/resources/repository.xml";
        ApplicationContext context = new FileSystemXmlApplicationContext(path);

        EntityManagerFactory emf = context.getBean("emf", EntityManagerFactory.class);
        EntityManager manager = emf.createEntityManager();
        EntityTransaction transaction = manager.getTransaction();
        transaction.begin();
        consumer.accept(manager);
        transaction.commit();
        manager.close();
    }

    @Transactional
    public void clearTable(String tableName) {
        executeDB(entityManager -> {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
            entityManager.createNativeQuery("TRUNCATE " + tableName).executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE " + tableName + " AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        });
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        clearTable("note");
    }
}
