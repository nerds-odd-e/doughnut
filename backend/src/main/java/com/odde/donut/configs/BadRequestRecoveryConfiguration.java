package com.odde.donut.configs;

import org.apache.catalina.core.StandardHost;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BadRequestRecoveryConfiguration
    implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
  @Override
  public void customize(TomcatServletWebServerFactory factory) {
    factory.addContextCustomizers(
        context ->
            ((StandardHost) context.getParent())
                .setErrorReportValveClass(BadRequestRecoveryValve.class.getName()));
  }
}
