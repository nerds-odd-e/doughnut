package com.odde.doughnut.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class ApplicationController {

    @GetMapping("/robots.txt")
    public void robots(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.getWriter().write("User-agent: *\n");
    }
}
