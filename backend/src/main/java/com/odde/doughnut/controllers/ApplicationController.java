package com.odde.doughnut.controllers;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        "/",
        "/bazaar/**",
        "/circles/**",
        "/notebooks/**",
        "/notes/**",
        "/reviews/**",
        "/answers/**",
        "/users/**",
        "/failure-report-list/**",
        "/links/**",
        "/dev-training-data/**"
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
