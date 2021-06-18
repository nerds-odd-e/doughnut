
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
        public List<User> users;
        public Integer badcount = 0;

    }
    @GetMapping("/backdoor")
    @Transactional
    public Res backdoor() {

        modelFactoryService.linkRepository.findAll().forEach(link-> {
            link.setTypeId(link.getLinkType().id);
            modelFactoryService.linkRepository.save(link);
        });

        Res r = new Res();
        modelFactoryService.linkRepository.findAll().forEach(link-> {
            if (link.getTypeId() == null)
                r.badcount += 1;
        });

        r.users = StreamSupport.stream(modelFactoryService.userRepository.findAll().spliterator(), false).collect(Collectors.toList());
        return r;
    }
}
