package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import com.odde.doughnut.entities.QuizQuestion.QuestionType;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_SOURCE_EXCLUSIVE;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_TARGET;

@Entity
@Table(name = "link")
public class Link {

    public enum LinkType {
        RELATED_TO("is related to", "is not related to", new QuestionType[0]),
        /* a subclass of */ BELONGS_TO("belongs to", "does not belong to", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        /* a superclass of */ HAS("has", "does not have", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),

        INSTANCE("is an instance of", "is not an instance of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        HAS_INSTANCE("has instances", "not have as an instance", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        /*INTEGRATED*/ PART("is a part of", "is not a part of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        HAS_PART("has parts", "not have as a part", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        /*NON INTEGRATED*/ TAGGED_BY("is tagged by", "is not tagged by", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        TAGGING("tagging", "is not tagging", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        ATTRIBUTE("is an attribute of", "is not an attribute of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        HAS_ATTRIBUTE("has attributes", "not has as an attribute", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),

        OPPOSITE_OF("is the opposite of", "is not the opposite of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        BROUGHT_BY("is brought by", "is not brought by", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        AUTHOR_OF("is author of", "is not author of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        USES("uses", "does not use", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        USED_BY("is used by", "is not used by", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        EXAMPLE_OF("is an example of", "is not an example of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        HAS_AS_EXAMPLE("has as example", "does not have as example", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        PRECEDES("precedes", "does not precede", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        SUCCEEDS("succeeds", "does not succeeds", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        SAME_AS("is the same as", "is not the same as", new QuestionType[]{LINK_TARGET}),
        SIMILAR_TO("is similar to", "is not similar to", new QuestionType[]{LINK_TARGET}),
        CONFUSE_WITH("confuses with", "does not confuse with", new QuestionType[0]);

        @JsonValue
        public final String label;
        public final String exclusiveQuestion;
        @Getter
        private final QuestionType[] questionTypes;

        LinkType(String label, String exclusiveQuestion, QuestionType[] questionTypes) {
            this.label = label;
            this.exclusiveQuestion = exclusiveQuestion;
            this.questionTypes = questionTypes;
        }

        public static LinkType fromString(String text) {
            for (LinkType b : LinkType.values()) {
                if (b.label.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }

        public LinkType reverseType() {
            if (this.equals(BELONGS_TO)) return HAS;
            if (this.equals(HAS)) return BELONGS_TO;
            if (this.equals(BROUGHT_BY)) return AUTHOR_OF;
            if (this.equals(AUTHOR_OF)) return BROUGHT_BY;
            if (this.equals(USES)) return USED_BY;
            if (this.equals(USED_BY)) return USES;
            if (this.equals(HAS_AS_EXAMPLE)) return EXAMPLE_OF;
            if (this.equals(EXAMPLE_OF)) return HAS_AS_EXAMPLE;
            if (this.equals(PRECEDES)) return SUCCEEDS;
            if (this.equals(SUCCEEDS)) return PRECEDES;
            if (this.equals(INSTANCE)) return HAS_INSTANCE;
            if (this.equals(HAS_INSTANCE)) return INSTANCE;
            if (this.equals(PART)) return HAS_PART;
            if (this.equals(HAS_PART)) return PART;
            if (this.equals(WITH_TAG)) return TAGGING;
            if (this.equals(TAGGING)) return WITH_TAG;
            if (this.equals(ATTRIBUTE)) return HAS_ATTRIBUTE;
            if (this.equals(HAS_ATTRIBUTE)) return ATTRIBUTE;
            return this;
        }
    }

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "source_id", referencedColumnName = "id")
    @JsonIgnoreProperties("noteContent")
    @Getter @Setter private Note sourceNote;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "target_id", referencedColumnName = "id")
    @JsonIgnoreProperties("noteContent")
    @Getter @Setter private Note targetNote;

    @Column(name = "type")
    @Getter
    @Setter
    private String type;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @Getter @Setter private User user;

    @Column(name = "created_at")
    @Getter
    @Setter
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @OneToMany(mappedBy = "link", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    private final List<ReviewPoint> reviewPointEntities = new ArrayList<>();

    public LinkType getLinkType() {
        return LinkType.fromString(type);
    }

    public void setLinkType(LinkType linkType) {
        if (linkType == null) {
            type = null;
        }
        type = linkType.label;
    }

    @JsonIgnore
    public List<Note> getBackwardPeers() {
        return targetNote.linkedNotesOfType(getLinkType().reverseType(), null);
    }

    @JsonIgnore
    public String getExclusiveQuestion() {
        return getLinkType().exclusiveQuestion;
    }

    @JsonIgnore
    public List<LinkType> getPossibleLinkTypes() {
        final List<LinkType> existingTypes = sourceNote.getLinks().stream().filter(l -> l.targetNote == targetNote).map(Link::getLinkType).collect(Collectors.toUnmodifiableList());
        return Arrays.stream(LinkType.values()).filter(lt->
                lt == getLinkType() || !existingTypes.contains(lt)).collect(Collectors.toList());
    }

    public boolean sourceVisibleAsTargetOrTo(User viewer) {
        if (sourceNote.getNotebook() == targetNote.getNotebook()) return true;
        if (viewer == null) return false;

        return viewer.canReferTo(sourceNote.getNotebook());
    }

}
