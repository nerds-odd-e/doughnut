package com.odde.doughnut;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class IndexController {
    @GetMapping("/")
    public String home(Map<String, Object> model) {
        model.put("name", "Nut, Doe");
        return "index";
    }
}
