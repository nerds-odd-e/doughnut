package com.odde.doughnut;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Controller
public class IndexController {
    @GetMapping("/")
    public String home(@AuthenticationPrincipal OAuth2User user, Model model) {
        if(user == null) {
            return "login";
        }
        model.addAttribute("name", user.getAttribute("name"));
        return "index";
    }
}
