package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.algorithms.SiblingOrder;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.hibernate.annotations.WhereJoinTable;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import javax.validation.Valid;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Entity
@Table(name = "note")
public class Note {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Embedded
    @Valid
    @Getter
    private NoteContent noteContent = new NoteContent();

    @Column(name = "sibling_order")
    private Long siblingOrder = SiblingOrder.getGoodEnoughOrderNumber();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "notebook_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    private Notebook notebook;

    @Column(name = "created_at")
    @Getter
    @Setter
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "master_review_setting_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private ReviewSetting masterReviewSetting;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private User user;

    @OneToMany(mappedBy = "sourceNote", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    @Getter
    @Setter
    private List<Link> links = new ArrayList<>();

    @OneToMany(mappedBy = "targetNote", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    @Getter
    @Setter
    private List<Link> refers = new ArrayList<>();

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL)
    @JsonIgnore
    @OrderBy("depth DESC")
    @Getter
    @Setter
    private List<NotesClosure> notesClosures = new ArrayList<>();

    @OneToMany(mappedBy = "ancestor", cascade = CascadeType.DETACH)
    @JsonIgnore
    @OrderBy("depth")
    @Getter
    @Setter
    private List<NotesClosure> descendantNCs = new ArrayList<>();

    @JoinTable(name = "notes_closure", joinColumns = {
            @JoinColumn(name = "ancestor_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)}, inverseJoinColumns = {
            @JoinColumn(name = "note_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
    })
    @OneToMany(cascade = CascadeType.DETACH)
    @JsonIgnore
    @WhereJoinTable(clause = "depth = 1")
    @OrderBy("sibling_order")
    @Getter
    private final List<Note> children = new ArrayList<>();

    @Override
    public String toString() {
        return "Note{" + "id=" + id + ", title='" + noteContent.getTitle() + '\'' + '}';
    }

    @JsonIgnore
    public List<Note> getTargetNotes() {
        return links.stream().map(Link::getTargetNote).collect(toList());
    }

    @JsonIgnore
    public Map<Link.LinkType, Map<String, List<Link>>> getAllLinks(User viewer) {
        return linkTypes(viewer).stream().collect(Collectors.toMap(x->x, x->new HashMap<String, List<Link>>(){{
                put("direct", linksOfTypeThroughDirect(x));
                put("reverse", linksOfTypeThroughReverse(x, viewer));
        }}));
    }

    public List<Link.LinkType> linkTypes(User viewer) {
        return Arrays.stream(Link.LinkType.values())
                .filter(t -> !linkedNotesOfType(t, viewer).isEmpty())
                .collect(Collectors.toUnmodifiableList());
    }

    public List<Link> linksOfTypeThroughDirect(Link.LinkType linkType) {
        return this.links.stream()
                .filter(l -> l.getLinkType().equals(linkType))
                .collect(Collectors.toList());
    }

    public List<Link> linksOfTypeThroughReverse(Link.LinkType linkType, User viewer) {
        return refers.stream()
                .filter(l -> l.getLinkType().equals(linkType.reverseType()))
                .filter(l -> l.sourceVisibleAsTargetOrTo(viewer))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<Note> linkedNotesOfType(Link.LinkType linkType, User viewer) {
        List<Note> notes = new ArrayList<>();
        linksOfTypeThroughDirect(linkType).forEach(lk -> notes.add(lk.getTargetNote()));
        linksOfTypeThroughReverse(linkType, viewer).forEach(lk -> notes.add(lk.getSourceNote()));
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

    @JsonIgnore
    public String getNoteTypeDisplay(){
        return this.getNotebook().getNotebookType().getDisplay();
    }

    private void addAncestors(List<Note> ancestors) {
        int[] counter = {1};
        ancestors.forEach(anc -> {
            NotesClosure notesClosure = new NotesClosure();
            notesClosure.setNote(this);
            notesClosure.setAncestor(anc);
            notesClosure.setDepth(counter[0]);
            getNotesClosures().add(0, notesClosure);
            counter[0] += 1;
        });
    }

    public void setParentNote(Note parentNote) {
        if (parentNote == null) return;
        notebook = parentNote.getNotebook();
        List<Note> ancestorsIncludingMe = parentNote.getAncestorsIncludingMe();
        Collections.reverse(ancestorsIncludingMe);
        addAncestors(ancestorsIncludingMe);
    }

    @JsonIgnore
    public List<Note> getAncestorsIncludingMe() {
        List<Note> ancestors = getAncestors();
        ancestors.add(this);
        return ancestors;
    }

    @JsonIgnore
    public List<Note> getAncestors() {
        return getNotesClosures().stream().map(NotesClosure::getAncestor).collect(toList());
    }

    public void traverseBreadthFirst(Consumer<Note> noteConsumer) {
        descendantNCs.stream().map(NotesClosure::getNote).forEach(noteConsumer);
    }

    @JsonIgnore
    public Note getParentNote() {
        List<Note> ancestors = getAncestors();
        if (ancestors.size() == 0) {
            return null;
        }
        return ancestors.get(ancestors.size() - 1);
    }

    @JsonIgnore
    public List<Note> getSiblings() {
        if (getParentNote() == null) {
            return new ArrayList<>();
        }
        return getParentNote().getChildren();
    }

    public String getTitle() {
        return noteContent.getTitle();
    }

    public void mergeMasterReviewSetting(ReviewSetting reviewSetting) {
        ReviewSetting current = getMasterReviewSetting();
        if (current == null) {
            setMasterReviewSetting(reviewSetting);
        } else {
            BeanUtils.copyProperties(reviewSetting, getMasterReviewSetting());
        }
    }

    public void updateNoteContent(NoteContent noteContent, User user) throws IOException {
        noteContent.fetchUploadedPicture(user);
        mergeNoteContent(noteContent);
    }

    public void mergeNoteContent(NoteContent noteContent) {
        if (noteContent.getUploadPicture() == null) {
            noteContent.setUploadPicture(getNoteContent().getUploadPicture());
        }
        BeanUtils.copyProperties(noteContent, getNoteContent());
    }

    @JsonIgnore
    public Note getPreviousSibling() {
        return getSiblings().stream()
                .filter(nc -> nc.siblingOrder < siblingOrder)
                .reduce((f, s) -> s).orElse(null);
    }

    @JsonIgnore
    public Note getNextSibling() {
        return getSiblings().stream()
                .filter(nc -> nc.siblingOrder > siblingOrder)
                .findFirst().orElse(null);
    }

    @JsonIgnore
    public Note getPrevious() {
        Note result = getPreviousSibling();
        if (result == null) {
            return getParentNote();
        }
        while (true) {
            List<Note> children = result.getChildren();
            if (children.size() == 0) {
                return result;
            }
            result = children.get(children.size() - 1);
        }
    }

    @JsonIgnore
    private Note getFirstChild() {
        return getChildren().stream().findFirst().orElse(null);
    }

    @JsonIgnore
    public Note getNext() {
        Note firstChild = getFirstChild();
        if (firstChild != null) {
            return firstChild;
        }
        Note next = this;
        while (next != null) {
            Note sibling = next.getNextSibling();
            if (sibling != null) {
                return sibling;
            }
            next = next.getParentNote();
        }
        return null;
    }

    public void updateSiblingOrder(Note relativeToNote, boolean asFirstChildOfNote) {
        Long newSiblingOrder = relativeToNote.theSiblingOrderItTakesToMoveRelativeToMe(asFirstChildOfNote);
        if (newSiblingOrder != null) {
            siblingOrder = newSiblingOrder;
        }
    }

    private long getSiblingOrderToInsertBehindMe() {
        Note nextSiblingNote = getNextSibling();
        Long relativeToSiblingOrder = siblingOrder;
        if (nextSiblingNote == null) {
            return relativeToSiblingOrder + SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
        }
        return (relativeToSiblingOrder + nextSiblingNote.siblingOrder) / 2;
    }

    private Long getSiblingOrderToBecomeMyFirstChild() {
        Note firstChild = getFirstChild();
        if (firstChild != null) {
            return firstChild.siblingOrder - SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
        }
        return null;
    }

    private Long theSiblingOrderItTakesToMoveRelativeToMe(boolean asFirstChildOfNote) {
        if (!asFirstChildOfNote) {
            return getSiblingOrderToInsertBehindMe();
        }
        return getSiblingOrderToBecomeMyFirstChild();
    }

    public boolean hasTitleInArticle() {
        if (hasNoDescriptionAndChild()) {
            return false;
        }
        return !noteContent.getHideTitleInArticle();
    }

    @JsonIgnore
    public String getArticleTitle() {
        if (hasNoDescriptionAndChild()) {
            return null;
        }
        return getTitle();
    }

    @JsonIgnore
    public String getArticleBody() {
        if (hasNoDescriptionAndChild()) {
            return getTitle();
        }
        return noteContent.getDescription();
    }

    private boolean hasNoDescriptionAndChild() {
        return Strings.isBlank(noteContent.getDescription()) && children.isEmpty();
    }

    public void buildNotebookForHeadNote(Ownership ownership, User creator) {
        final Notebook notebook = new Notebook();
        notebook.setCreatorEntity(creator);
        notebook.setOwnership(ownership);
        notebook.setHeadNote(this);

        this.user = creator;
        this.notebook = notebook;
    }

}

