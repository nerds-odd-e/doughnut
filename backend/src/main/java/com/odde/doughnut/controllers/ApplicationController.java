package com.odde.doughnut.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ApplicationController {

  @GetMapping("/robots.txt")
  public void robots(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.getWriter().write("User-agent: *\n");
  }

  // This backend route is to trigger the authentication process to identify the user.
  // In production, we use OAuth2 to identify the user.
  // In non-production, we use frontend to identify the user.
  @GetMapping("/users/identify")
  public String identify(
      @RequestParam(name = "from", required = false, defaultValue = "/") String from) {
    return "redirect:" + from;
  }
}
