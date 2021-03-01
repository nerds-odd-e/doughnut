package com.odde.doughnut.testability;

import org.hibernate.Metamodel;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Table;
import javax.persistence.metamodel.EntityType;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DBCleanerWorker {
    private final EntityManagerFactory emf;

    public DBCleanerWorker(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Transactional
    public void truncateAllTables() {
        withEntityManager(entityManager
                -> getAnnotatedTableNames(entityManager)
                .forEach(t -> truncateTable(t, entityManager)));
    }

    private EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    private void withEntityManager(Consumer<EntityManager> consumer) {
        EntityManager manager = createEntityManager();
        EntityTransaction transaction = manager.getTransaction();
        transaction.begin();
        consumer.accept(manager);
        transaction.commit();
        manager.close();
    }

    private void truncateTable(String tableName, EntityManager entityManager) {
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=0").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE `" + tableName + "`").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE `" + tableName + "`").executeUpdate();
        entityManager.createNativeQuery("ALTER TABLE `" + tableName + "` AUTO_INCREMENT=1").executeUpdate();
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
    }

    private List<String> getAnnotatedTableNames(EntityManager manager) {
        Metamodel metamodel = (Metamodel) manager.getMetamodel();
        Set<EntityType<?>> entities = metamodel.getEntities();

        return entities.stream()
                .map(e->{
                    Table annotation = e.getJavaType().getAnnotation(Table.class);
                    return annotation.name();
                })
                .collect(Collectors.toList());
    }

    public static String camelToSnake(String camel) {
        String underscore;
        underscore = String.valueOf(Character.toLowerCase(camel.charAt(0)));
        for (int i = 1; i < camel.length(); i++) {
            underscore += Character.isLowerCase(camel.charAt(i)) ? String.valueOf(camel.charAt(i))
                    : "_" + Character.toLowerCase(camel.charAt(i));
        }
        return underscore;
    }
}