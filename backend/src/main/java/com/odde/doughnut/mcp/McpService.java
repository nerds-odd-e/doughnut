package com.odde.doughnut.mcp;

import io.micrometer.common.util.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class McpService {
  @Autowired private Environment env;
  private RestTemplate restTemplate = new RestTemplate();
  private String urlLocal = "http://localhost:9081/";
  private String urlProd = "https://doughnut.odd-e.com/";

  private String api = "/api/user/info";

  // This should get when Mcp server first start
  // Could get from file
  private String mcpToken = "TestToken";

  @Tool(description = "Get instruction")
  public String getInstruction() {
    return "Doughnut is a Personal Knowledge Management tool";
  }

  @Tool(description = "Get user profile")
  public String getUserInfo() {

    // if not login then login
    String url = this.urlLocal + this.api;
    if (env.acceptsProfiles(Profiles.of("prod"))) {
      url = this.urlProd + this.api;
    }
    HttpHeaders headers = new HttpHeaders();
    headers.set("mcpToken", this.mcpToken);
    if (StringUtils.isEmpty(this.mcpToken)) {
      return "ERROR: empty token in mcp service";
    }
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

    if (response.getStatusCode() == HttpStatus.OK) {
      return response.getBody();
    }

    return "";
  }
}
