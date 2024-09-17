package com.odde.doughnut.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ApplicationController {
  @Autowired private Environment env;

  @GetMapping("/robots.txt")
  public void robots(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.getWriter().write("User-agent: *\n");
  }

  @RequestMapping(
      value = {
        "/",
        // most of the frontend routes start with /d/ except for notes which start with /n
        "/d/**",
        "/n**",
      },
      method = RequestMethod.GET)
  public String home() {
    return "/index.html";
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
