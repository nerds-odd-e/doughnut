package com.odde.doughnut.testability;

import com.odde.doughnut.entities.repositories.SubscriptionRepository;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;

@RestController
public class CheapBackupController {
    private final ModelFactoryService modelFactoryService;
    private final SubscriptionRepository subscriptionRepository;

    public CheapBackupController(ModelFactoryService modelFactoryService, SubscriptionRepository subscriptionRepository) {
        this.modelFactoryService = modelFactoryService;
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping("/api/backup")
    @Transactional
    public HashMap<String, Object> backup() {
        HashMap<String, Object> hash = new HashMap<>();

        subscriptionRepository.findAll().forEach(s->{
            s.setNotebookEntity(s.getNoteEntity().getNotebookEntity());
            subscriptionRepository.save(s);
            hash.put("sb"+ s.getId(), s.getNotebookEntity());
        });

        modelFactoryService.bazaarNotebookRepository.findAll().forEach(bn->{
            bn.setNotebookEntity(bn.getNote().getNotebookEntity());
            modelFactoryService.bazaarNotebookRepository.save(bn);
            hash.put("bz"+ bn.getId(), bn.getNotebookEntity());
        });

        return hash;
    }

    @GetMapping("/api/db_migration_history")
    public List dbM(Model model) {
        return modelFactoryService.entityManager.createNativeQuery("select * from flyway_schema_history").getResultList();
    }

}

