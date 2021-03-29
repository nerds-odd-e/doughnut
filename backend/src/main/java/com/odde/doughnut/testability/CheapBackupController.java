package com.odde.doughnut.testability;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.NotesClosureEntity;
import com.odde.doughnut.models.TreeNodeModel;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
public class CheapBackupController {
    private final ModelFactoryService modelFactoryService;

    public CheapBackupController(ModelFactoryService modelFactoryService) {
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/api/backup")
    public HashMap<String, Object> backup(Model model) {
        modelFactoryService.noteRepository.findAll().forEach(n->{
            TreeNodeModel node = modelFactoryService.toTreeNodeModel(n);
            int[] counter = {0};
            node.getAncestors().forEach(anc->{
                if(counter[0] > 0) {
                    NotesClosureEntity notesClosureEntity = new NotesClosureEntity();
                    notesClosureEntity.setNoteEntity(n);
                    notesClosureEntity.setAncestorEntity(anc);
                    notesClosureEntity.setDepth(counter[0]);
                    modelFactoryService.entityManager.persist(notesClosureEntity);
                }
                counter[0] += 1;

            });
        });
        HashMap<String, Object> hash = new HashMap<>();
        hash.put("users", modelFactoryService.userRepository.findAll());
        hash.put("notes", modelFactoryService.noteRepository.findAll());
        hash.put("review_points", modelFactoryService.reviewPointRepository.findAll());
        return hash;
    }

    @GetMapping("/api/db_migration_history")
    public List dbM(Model model) {
        return modelFactoryService.entityManager.createNativeQuery("select * from flyway_schema_history").getResultList();
    }

}

