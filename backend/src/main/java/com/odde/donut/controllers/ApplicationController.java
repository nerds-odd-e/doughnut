package com.odde.donut.controllers;

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

  @GetMapping("/login/continue")
  public String loginContinue(
      @RequestParam(name = "from", required = false, defaultValue = "/") String from) {
    return "redirect:" + from;
  }
}
