
package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
    public String dataUpgrade() {
        modelFactoryService.linkRepository.findAll().forEach(l->{
            if (List.of(GENERALIZE, HAS_INSTANCE, HAS_PART, TAGGING, HAS_INSTANCE, BROUGHT_BY, USED_BY, HAS_AS_EXAMPLE, SUCCEEDS).contains(l.getLinkType())) {
                Note temp = l.getSourceNote();
                l.setSourceNote(l.getTargetNote());
                l.setTargetNote(temp);
            }
            if (l.getLinkType() == GENERALIZE) {
                l.setLinkType(SPECIALIZE);
            }
            if (l.getLinkType() == HAS_INSTANCE) {
                l.setLinkType(INSTANCE);
            }
            if (l.getLinkType() == HAS_PART) {
                l.setLinkType(PART);
            }
            if (l.getLinkType() == TAGGING) {
                l.setLinkType(TAGGED_BY);
            }
            if (l.getLinkType() == HAS_ATTRIBUTE) {
                l.setLinkType(ATTRIBUTE);
            }
            if (l.getLinkType() == BROUGHT_BY) {
                l.setLinkType(AUTHOR_OF);
            }
            if (l.getLinkType() == USED_BY) {
                l.setLinkType(USES);
            }
            if (l.getLinkType() == HAS_AS_EXAMPLE) {
                l.setLinkType(EXAMPLE_OF);
            }
            if (l.getLinkType() == SUCCEEDS) {
                l.setLinkType(PRECEDES);
            }
            modelFactoryService.linkRepository.save(l);
        });

        return "OK. Active Profile: " + String.join(", ", environment.getActiveProfiles());
    }

}
