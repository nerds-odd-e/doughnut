package com.odde.doughnut.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

@Controller
public class ApplicationController {
  private final Environment env;
  private final RestTemplate restTemplate;
  private final String spaPublicBaseUrl;

  public ApplicationController(
      Environment env,
      RestTemplate restTemplate,
      @Value("${doughnut.spa-public-base-url:https://doughnut.odd-e.com}")
          String spaPublicBaseUrl) {
    this.env = env;
    this.restTemplate = restTemplate;
    this.spaPublicBaseUrl = spaPublicBaseUrl;
  }

  @GetMapping("/robots.txt")
  public void robots(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.getWriter().write("User-agent: *\n");
  }

  @GetMapping("/")
  public String home() {
    return "/index.html";
  }

  /**
   * In prod, the load balancer sends {@code /} and {@code /assets/*} to GCS but deep links here
   * still hit the MIG. Serving classpath {@code index.html} would desync chunk names from the
   * active GCS tree when the jar and URL-map SHA differ; fetch the same shell the browser gets on
   * {@code /}.
   */
  @RequestMapping(
      value = {
        "/d/**", "/n**",
      },
      method = RequestMethod.GET)
  public Object spaDeepLink() {
    if (env.acceptsProfiles(Profiles.of("prod"))) {
      return fetchSpaShellFromPublicOrigin();
    }
    return "/index.html";
  }

  private ResponseEntity<byte[]> fetchSpaShellFromPublicOrigin() {
    String base =
        spaPublicBaseUrl.endsWith("/")
            ? spaPublicBaseUrl.substring(0, spaPublicBaseUrl.length() - 1)
            : spaPublicBaseUrl;
    URI uri = URI.create(base + "/");
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.setAccept(MediaType.parseMediaTypes(MediaType.TEXT_HTML_VALUE));
    ResponseEntity<byte[]> upstream =
        restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(requestHeaders), byte[].class);
    HttpHeaders responseHeaders = new HttpHeaders();
    if (upstream.getHeaders().getContentType() != null) {
      responseHeaders.setContentType(upstream.getHeaders().getContentType());
    } else {
      responseHeaders.setContentType(MediaType.TEXT_HTML);
    }
    return new ResponseEntity<>(upstream.getBody(), responseHeaders, upstream.getStatusCode());
  }

  // This backend route is to trigger the authentication process to identify the user.
  // In production, we use OAuth2 to identify the user.
  // In non-production, we use frontend to identify the user.
  @GetMapping("/users/identify")
  public String identify(
      @RequestParam(name = "from", required = false, defaultValue = "/") String from) {
    if (env.acceptsProfiles(Profiles.of("prod"))) {
      return "redirect:" + from;
    }

    return home();
  }
}
