package com.odde.doughnut.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class ApplicationController {

  @GetMapping("/robots.txt")
  public void robots(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.getWriter().write("User-agent: *\n");
  }

  @RequestMapping(
      value = {
        // the following array has to be in sync with the frontend routes in
        // CommonConfiguration.java
        // Because java annotation does not allow variable, we have to repeat the routes here.
        "/",
        "/bazaar/**",
        "/circles/**",
        "/notebooks/**",
        "/notes/**",
        "/reviews/**",
        "/answers/**",
        "/links/**",
        "/failure-report-list/**",
        "/admin-dashboard/**"
      },
      method = RequestMethod.GET)
  public String home() {
    return "vuejsed";
  }

  @GetMapping("/users/identify")
  public RedirectView identify(@RequestParam String from) {
    return new RedirectView(from);
  }
}
