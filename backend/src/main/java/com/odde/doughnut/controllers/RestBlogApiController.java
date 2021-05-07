
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.BlogModel;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blog")
class RestBlogApiController {
  private final ModelFactoryService modelFactoryService;

  public RestBlogApiController(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  @GetMapping("/posts_by_website_name/{websiteName}")
  public List<BlogPost> getBlogPostsByWebsiteName() {
    Note note =
        modelFactoryService.noteRepository.findFirstByTitle("odd-e-blog");
    BlogModel blogModel = modelFactoryService.toBlogModel(note.getNotebook());
    return blogModel.getBlogPosts();
  }

  @GetMapping("/year_list")
  public List<String> getBlogYearMonthList() {
    Note note =
        modelFactoryService.noteRepository.findFirstByTitle("odd-e-blog");
    BlogModel blogModel = modelFactoryService.toBlogModel(note.getNotebook());

    return blogModel.getBlogYears(note);
  }
}
