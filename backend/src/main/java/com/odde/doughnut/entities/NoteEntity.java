package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.algorithms.SiblingOrder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.WhereJoinTable;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Entity
@Table(name = "note")
public class NoteEntity {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Embedded
    @Valid
    @Getter
    @Setter
    private NoteContentEntity noteContent = new NoteContentEntity();

    @Column(name = "sibling_order")
    @Getter
    @Setter
    private Long siblingOrder = SiblingOrder.getGoodEnoughOrderNumber();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "ownership_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private OwnershipEntity ownershipEntity;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "master_review_setting_id", referencedColumnName = "id")
    @Getter
    @Setter
    private ReviewSettingEntity masterReviewSettingEntity;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private UserEntity userEntity;

    @OneToMany(mappedBy = "sourceNote", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    @Getter
    @Setter
    private List<LinkEntity> links = new ArrayList<>();

    @OneToMany(mappedBy = "targetNote", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    @Getter
    @Setter
    private List<LinkEntity> refers = new ArrayList<>();

    @OneToMany(mappedBy = "noteEntity", cascade = CascadeType.ALL)
    @JsonIgnore
    @OrderBy("depth DESC")
    @Getter
    @Setter
    private List<NotesClosureEntity> notesClosures = new ArrayList<>();

    @OneToMany(mappedBy = "ancestorEntity", cascade = CascadeType.DETACH)
    @JsonIgnore
    @OrderBy("depth")
    @Getter
    @Setter
    private List<NotesClosureEntity> descendantNCs = new ArrayList<>();

    @JoinTable(name = "notes_closure", joinColumns = {
            @JoinColumn(name = "ancestor_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)}, inverseJoinColumns = {
            @JoinColumn(name = "note_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
    })
    @OneToMany(cascade = CascadeType.DETACH)
    @JsonIgnore
    @WhereJoinTable(clause = "depth = 1")
    @OrderBy("sibling_order")
    @Getter
    private final List<NoteEntity> children = new ArrayList<>();

    @Override
    public String toString() {
        return "Note{" + "id=" + id + ", title='" + noteContent.getTitle() + '\'' + '}';
    }

    public List<NoteEntity> getTargetNotes() {
        return links.stream().map(LinkEntity::getTargetNote).collect(toList());
    }

    public List<LinkEntity.LinkType> linkTypes() {
        return Arrays.stream(LinkEntity.LinkType.values())
                .filter(t -> !linkedNotesOfType(t).isEmpty())
                .collect(Collectors.toUnmodifiableList());
    }

    public List<LinkEntity> linksOfTypeThroughDirect(LinkEntity.LinkType linkType) {
        return this.links.stream()
                .filter(l -> l.getLinkType().equals(linkType))
                .collect(Collectors.toList());
    }

    public List<LinkEntity> linksOfTypeThroughReverse(LinkEntity.LinkType linkType) {
        return refers.stream()
                .filter(l -> l.getLinkType().equals(linkType.reverseType()))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<NoteEntity> linkedNotesOfType(LinkEntity.LinkType linkType) {
        List<NoteEntity> notes = new ArrayList<>();
        linksOfTypeThroughDirect(linkType).forEach(lk -> notes.add(lk.getTargetNote()));
        linksOfTypeThroughReverse(linkType).forEach(lk -> notes.add(lk.getSourceNote()));
        return notes;
    }

    public String getNotePicture() {
        if (noteContent.getUseParentPicture() && getParentNote() != null) {
            return getParentNote().getNotePicture();
        }
        return noteContent.getNotePicture();
    }

    public boolean isHead() {
        return getParentNote() == null;
    }

    public void addAncestors(List<NoteEntity> ancestors) {
        int[] counter = {1};
        ancestors.forEach(anc -> {
            NotesClosureEntity notesClosureEntity = new NotesClosureEntity();
            notesClosureEntity.setNoteEntity(this);
            notesClosureEntity.setAncestorEntity(anc);
            notesClosureEntity.setDepth(counter[0]);
            getNotesClosures().add(0, notesClosureEntity);
            counter[0] += 1;
        });
    }

    public void setParentNote(NoteEntity parentNote) {
        if (parentNote == null) return;
        List<NoteEntity> ancestorsIncludingMe = parentNote.getAncestorsIncludingMe();
        Collections.reverse(ancestorsIncludingMe);
        addAncestors(ancestorsIncludingMe);
    }

    public List<NoteEntity> getAncestorsIncludingMe() {
        List<NoteEntity> ancestors = getAncestors();
        ancestors.add(this);
        return ancestors;
    }

    public List<NoteEntity> getAncestors() {
        return getNotesClosures().stream().map(NotesClosureEntity::getAncestorEntity).collect(toList());
    }

    public void traverseBreadthFirst(Consumer<NoteEntity> noteEntityConsumer) {
        descendantNCs.stream().map(NotesClosureEntity::getNoteEntity).forEach(noteEntityConsumer);
    }

    public NoteEntity getParentNote() {
        List<NoteEntity> ancestors = getAncestors();
        if (ancestors.size() == 0) {
            return null;
        }
        return ancestors.get(ancestors.size() - 1);
    }

    public List<NoteEntity> getSiblings() {
        if (getParentNote() == null) {
            return new ArrayList<>();
        }
        return getParentNote().getChildren();
    }

    public String getTitle() {
        return noteContent.getTitle();
    }

    public void mergeMasterReviewSetting(ReviewSettingEntity reviewSettingEntity) {
        ReviewSettingEntity current = getMasterReviewSettingEntity();
        if (current == null) {
            setMasterReviewSettingEntity(reviewSettingEntity);
        } else {
            BeanUtils.copyProperties(reviewSettingEntity, getMasterReviewSettingEntity());
        }
    }

    public void populate(OwnershipEntity ownershipEntity, NoteEntity parentNote, NoteContentEntity noteContentEntity, UserEntity userEntity) throws IOException {
        updateNoteContent(noteContentEntity, userEntity);
        setParentNote(parentNote);
        setOwnershipEntity(ownershipEntity);
        if (ownershipEntity == null) {
            setOwnershipEntity(parentNote.getOwnershipEntity());
        }
        setUserEntity(userEntity);
    }

    public void updateNoteContent(NoteContentEntity noteContentEntity, UserEntity userEntity) throws IOException {
        setNoteContent(noteContentEntity);
        noteContent.fetchUploadedPicture(userEntity);
    }
}
