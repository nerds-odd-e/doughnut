package com.odde.doughnut.configs;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.firewall.StrictHttpFirewall;

/**
 * Note deep links encode each slug segment and join with {@code /}, so hierarchical slugs contain
 * {@code %2F} in the path. Spring Security's default {@link StrictHttpFirewall} and Tomcat's
 * default handling of encoded solidus can reject that with HTTP 400.
 */
@Configuration
public class AllowUrlEncodedSlashHttpFirewallConfiguration {

  @Bean
  WebSecurityCustomizer allowUrlEncodedSlashInPath() {
    return web -> {
      StrictHttpFirewall firewall = new StrictHttpFirewall();
      firewall.setAllowUrlEncodedSlash(true);
      web.httpFirewall(firewall);
    };
  }

  @Bean
  WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatEncodedSolidusDecode() {
    return factory ->
        factory.addConnectorCustomizers(
            (Connector connector) -> connector.setEncodedSolidusHandling("decode"));
  }
}
