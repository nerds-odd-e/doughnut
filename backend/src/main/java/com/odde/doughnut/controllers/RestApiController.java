
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.BlogModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:8000")
@RequestMapping("/api")
class RestApiController {
    @Autowired
    private Environment environment;

    private final ModelFactoryService modelFactoryService;

    public RestApiController(ModelFactoryService modelFactoryService) {
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/healthcheck")
    public String ping() {
        return "OK. Active Profile: " + String.join(", ", environment.getActiveProfiles());
    }

    @GetMapping("/bazaar_notes")
    public List<Notebook> getBazaarNotes() {
        BazaarModel bazaarModel = modelFactoryService.toBazaarModel();
        return bazaarModel.getAllNotebooks();
    }

    @GetMapping("/blog_posts_by_website_name/{websiteName}")
    public List<BlogArticle> getBlogArticlesByWebsiteName(@PathVariable String websiteName) {
        Note note = modelFactoryService.noteRepository.findFirstByTitle("odd-e-blog");
        BlogModel blogModel = modelFactoryService.toBlogModel(note);
        BlogYearMonth targetYearMonth = new BlogYearMonth("2021", "Apr");
        return blogModel.getBlogArticles(note,targetYearMonth);
    }

    @GetMapping("/blog/yearmonth")
    public List<BlogYearMonth> getBlogYearMonthList() {
        Note note = modelFactoryService.noteRepository.findFirstByTitle("odd-e-blog");
        BlogModel blogModel = modelFactoryService.toBlogModel(note);
        List<BlogYearMonth> yearMonths = blogModel.getBlogYearMonths(note);

        return yearMonths;
    }
}
