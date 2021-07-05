package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.exceptions.NoAccessRightException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/links")
public class LinkController extends ApplicationMvcController  {

    public LinkController(CurrentUserFetcher currentUserFetcher) {
        super(currentUserFetcher);
    }

    @GetMapping("/{link}")
    public String show(@PathVariable("link") Link link) throws NoAccessRightException {
        currentUserFetcher.getUser().getAuthorization().assertAuthorization(link);
        return "vuejsed";
    }
}
