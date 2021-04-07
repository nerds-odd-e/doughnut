package com.odde.doughnut.testability;

import com.odde.doughnut.entities.NotebookEntity;
import com.odde.doughnut.entities.NoteEntity;
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

    public CheapBackupController(ModelFactoryService modelFactoryService) {
        this.modelFactoryService = modelFactoryService;
    }

    @GetMapping("/api/backup")
    @Transactional
    public HashMap<String, Object> backup() {
        HashMap<String, Object> hash = new HashMap<>();

        modelFactoryService.noteRepository.findAll().forEach(note->{
            if (note.getParentNote() == null && note.getNotebookEntity() == null) {
                NotebookEntity noteBookEntity = new NotebookEntity();
                noteBookEntity.setOwnershipEntity(note.getOwnershipEntity());
                noteBookEntity.setCreatorEntity(note.getUserEntity());
                noteBookEntity.setHeadNoteEntity(note);
                note.setNotebookEntity(noteBookEntity);
                modelFactoryService.noteRepository.save(note);
            }
        });

        modelFactoryService.noteRepository.findAll().forEach(note->{
            if (note.getParentNote() == null) {
                note.getDescendantNCs().forEach(nc->{
                    NoteEntity n = nc.getNoteEntity();
                    n.setNotebookEntity(note.getNotebookEntity());
                    modelFactoryService.noteRepository.save(n);
                });
            }
        });

        int cnt[] = {0};
        modelFactoryService.noteRepository.findAll().forEach(note->{
            if (note.getNotebookEntity() == null) {
                cnt[0]+=1;
            }
        });
        hash.put("dangling", cnt[0]);

        return hash;
    }

    @GetMapping("/api/db_migration_history")
    public List dbM(Model model) {
        return modelFactoryService.entityManager.createNativeQuery("select * from flyway_schema_history").getResultList();
    }

}

