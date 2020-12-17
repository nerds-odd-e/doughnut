package com.odde.doughnut;

import org.junit.jupiter.api.extension.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.function.Consumer;

public class ApplicationContextWithRepositories implements BeforeAllCallback, ParameterResolver {
    private ApplicationContext applicationContext;

    public void executeDB(Consumer<EntityManager> consumer) {
        ApplicationContext context = getApplicationContext();

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

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(ApplicationContext.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        ApplicationContext context = getApplicationContext();
        return context;
    }

    private ApplicationContext getApplicationContext() {
        String path = "/src/test/resources/repository.xml";
        if (applicationContext == null) {
            this.applicationContext = new FileSystemXmlApplicationContext(path);
        }
        return applicationContext;
    }
}
