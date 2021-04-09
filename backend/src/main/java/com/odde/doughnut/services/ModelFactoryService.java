package com.odde.doughnut.services;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.*;
import com.odde.doughnut.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.validation.Valid;
import java.util.Optional;

@Service
public class ModelFactoryService {
    @Autowired public NoteRepository noteRepository;
    @Autowired public UserRepository userRepository;
    @Autowired public BazaarNotebookRepository bazaarNotebookRepository;
    @Autowired public ReviewPointRepository reviewPointRepository;
    @Autowired public CircleRepository circleRepository;
    @Autowired public LinkRepository linkRepository;
    @Autowired public NotesClosureRepository notesClosureRepository;
    @Autowired public NotebookRepository notebookRepository;
    @Autowired public EntityManager entityManager;

    public TreeNodeModel toTreeNodeModel(Note note) {
        return new TreeNodeModel(note, this);
    }

    public NoteMotionModel toNoteMotionModel(NoteMotionEntity noteMotionEntity, Note note) {
        noteMotionEntity.setSubject(note);
        return new NoteMotionModel(noteMotionEntity, this);
    }

    public BazaarModel toBazaarModel() {
        return new BazaarModel(this);
    }

    public Optional<UserEntity> findUserById(Integer id) {
        return userRepository.findById(id);
    }

    public Optional<Note> findNoteById(Integer noteId) {
        return noteRepository.findById(noteId);
    }

    public UserModel toUserModel(UserEntity userEntity) {
        if (userEntity == null) {
            return null;
        }
        return new UserModel(userEntity, this);
    }

    public ReviewPointModel toReviewPointModel(@Valid ReviewPointEntity reviewPointEntity) {
        return new ReviewPointModel(reviewPointEntity, this);
    }

    public <T, M extends ModelForEntity<T>> M toModel(T entity, Class<M> klass){
        if (klass == ReviewPointModel.class) {
            return (M) toReviewPointModel((ReviewPointEntity) entity);
        }
        if (klass == UserModel.class) {
            return (M) toUserModel((UserEntity) entity);
        }
        return null;
    }

    public CircleModel toCircleModel(CircleEntity circleEntity) {
        return new CircleModel(circleEntity, this);
    }

    public CircleModel findCircleByInvitationCode(String invitationCode) {
        CircleEntity circleEntity = circleRepository.findFirstByInvitationCode(invitationCode);
        if (circleEntity == null) {
            return null;
        }
        return toCircleModel(circleEntity);
    }

    public AnswerModel toAnswerModel(Answer answer) {
        return new AnswerModel(answer, this);
    }

    public LinkModel toLinkModel(LinkEntity linkEntity) {
        return new LinkModel(linkEntity, this);
    }

    public SubscriptionModel toSubscriptionModel(SubscriptionEntity sub) {
        return new SubscriptionModel(sub, this);
    }
}
