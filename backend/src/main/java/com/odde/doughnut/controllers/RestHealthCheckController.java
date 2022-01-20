
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.NoteContent;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.TextContent;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
class RestHealthCheckController {
    @Autowired
    private Environment environment;

    @Autowired
    private ModelFactoryService modelFactoryService;

    @GetMapping("/healthcheck")
    public String ping() {
        return "OK. Active Profile: " + String.join(", ", environment.getActiveProfiles());
    }

    @GetMapping("/data_upgrade")
    @Transactional(timeout=200)
    public List dataUpgrade() {
        modelFactoryService.entityManager.createNativeQuery("delete from text_content where id not in (select text_content_id from note)").executeUpdate();
        long count = modelFactoryService.textContentRepository.count();
        long countNotes = modelFactoryService.noteRepository.count();

        return List.of(count, countNotes);
    }

}
