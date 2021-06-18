
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api")
class RestHealthCheckController {
    private final ModelFactoryService modelFactoryService;

    @Autowired
    private Environment environment;

    RestHealthCheckController(ModelFactoryService modelFactoryService) {
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/healthcheck")
    public String ping() {
        return "OK. Active Profile: " + String.join(", ", environment.getActiveProfiles());
    }

    class Res {
        public Note note8;
        public Note note9;
        public Note note55;
        public Note note56;

    }
    @GetMapping("/backdoor")
    @Transactional
    public Res backdoor() {

        Res r= new Res();
        r.note8 = modelFactoryService.notebookRepository.findById(8).orElse(null).getHeadNote();
        r.note9 = modelFactoryService.notebookRepository.findById(9).orElse(null).getHeadNote();
        r.note55 = modelFactoryService.notebookRepository.findById(55).orElse(null).getHeadNote();
        r.note56 = modelFactoryService.notebookRepository.findById(56).orElse(null).getHeadNote();
        return r;
    }
}
