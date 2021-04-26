
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.BlogModel;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:8000")
@RequestMapping("/api/blog")
class RestBlogApiController {
    private final ModelFactoryService modelFactoryService;

    public RestBlogApiController(ModelFactoryService modelFactoryService) {
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/posts_by_website_name/{websiteName}")
    public List<BlogPost> getBlogPostsByWebsiteName() {
        Note note = modelFactoryService.noteRepository.findFirstByTitle("odd-e-blog");
        BlogModel blogModel = modelFactoryService.toBlogModel(note);
        return blogModel.getBlogPosts(note);
    }

    @GetMapping("/year_list")
    public List<String> getBlogYearMonthList() {
        Note note = modelFactoryService.noteRepository.findFirstByTitle("odd-e-blog");
        BlogModel blogModel = modelFactoryService.toBlogModel(note);

        return blogModel.getBlogYears(note);
    }
}
