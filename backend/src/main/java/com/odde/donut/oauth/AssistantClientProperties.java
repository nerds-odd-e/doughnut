package com.odde.donut.oauth;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "donut.oauth")
public class AssistantClientProperties {
  private List<Client> clients = new ArrayList<>();

  @Getter
  @Setter
  public static class Client {
    private String clientId;
    private String clientName;

    /** Encoded secret accepted by Spring Security's delegating password encoder. */
    private String clientSecret;

    private List<String> redirectUris = new ArrayList<>();
  }
}
