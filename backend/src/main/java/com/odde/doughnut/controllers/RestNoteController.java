
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.BlogPost;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.BlogModel;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
class RestNoteController {
  private final ModelFactoryService modelFactoryService;

  public RestNoteController(ModelFactoryService modelFactoryService) {
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
