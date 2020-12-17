package com.odde.doughnut.testability;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

class ApplicationContextForRepositoriesHolder {
    private static final ApplicationContext applicationContext;

    static {
        String path = "/src/test/resources/repository.xml";
        applicationContext = new FileSystemXmlApplicationContext(path);
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
