
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.odde.doughnut.entities.Link.LinkType.*;

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
    @Transactional
    public String dataUpgrade() {
        modelFactoryService.linkRepository.findAll().forEach(l->{

            if (l.getLinkType() == SAME_AS) {
                l.setLinkType(SIMILAR_TO);
                modelFactoryService.linkRepository.save(l);
            }
        });

        return "OK. Active Profile: " + String.join(", ", environment.getActiveProfiles());
    }

}
