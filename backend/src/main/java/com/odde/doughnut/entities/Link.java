package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import com.odde.doughnut.entities.QuizQuestion.QuestionType;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_SOURCE_EXCLUSIVE;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_TARGET;

@Entity
@Table(name = "link")
public class Link {

    public enum LinkType {
        RELATED_TO(1, "is related to", "is not related to", new QuestionType[0]),
        SPECIALIZE(2, "is a specialization of", "is not a specialization of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        GENERALIZE(3, "is a generalization of", "is not a generalization of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),

        INSTANCE(4, "is an instance of", "is not an instance of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        HAS_INSTANCE(5, "has instances", "not have as an instance", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        /*INTEGRATED*/ PART(6, "is a part of", "is not a part of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        HAS_PART(7, "has parts", "not have as a part", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        /*NON INTEGRATED*/ TAGGED_BY(8, "is tagged by", "is not tagged by", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        TAGGING(9, "tagging", "is not tagging", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        ATTRIBUTE(10, "is an attribute of", "is not an attribute of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        HAS_ATTRIBUTE(11, "has attributes", "not has as an attribute", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),

        OPPOSITE_OF(12, "is the opposite of", "is not the opposite of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        BROUGHT_BY(13, "is brought by", "is not brought by", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        AUTHOR_OF(14, "is author of", "is not author of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        USES(15, "uses", "does not use", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        USED_BY(16, "is used by", "is not used by", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        EXAMPLE_OF(17, "is an example of", "is not an example of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        HAS_AS_EXAMPLE(18, "has as example", "does not have as example", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        PRECEDES(19, "precedes", "does not precede", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        SUCCEEDS(20, "succeeds", "does not succeeds", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        SAME_AS(21, "is the same as", "is not the same as", new QuestionType[]{LINK_TARGET}),
        SIMILAR_TO(22, "is similar to", "is not similar to", new QuestionType[]{LINK_TARGET}),
        CONFUSE_WITH(23, "confuses with", "does not confuse with", new QuestionType[0]);

        @JsonValue
        public final String label;
        public final Integer id;
        public final String exclusiveQuestion;
        @Getter
        private final QuestionType[] questionTypes;

        LinkType(Integer id, String label, String exclusiveQuestion, QuestionType[] questionTypes) {
            this.label = label;
            this.id = id;
            this.exclusiveQuestion = exclusiveQuestion;
            this.questionTypes = questionTypes;
        }

        private static final Map<Integer, LinkType> idMap = Collections.unmodifiableMap(Arrays.stream(values()).collect(Collectors.toMap(x->x.id, x->x)));

        public static LinkType fromLabel(String text) {
            for (LinkType b : LinkType.values()) {
                if (b.label.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }

        public static LinkType fromId(Integer id) {
            return idMap.getOrDefault(id, null);
        }

        public LinkType reverseType() {
            if (this.equals(SPECIALIZE)) return GENERALIZE;
            if (this.equals(GENERALIZE)) return SPECIALIZE;
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
            if (this.equals(TAGGED_BY)) return TAGGING;
            if (this.equals(TAGGING)) return TAGGED_BY;
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

    @Column(name = "type_id")
    @Getter
    @Setter
    @JsonIgnore
    private Integer typeId;

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

    @JsonIgnore
    public LinkType getLinkType() {
        return LinkType.fromId(typeId);
    }

    public String getLinkTypeLabel() {
        return getLinkType().label;
    }

    public void setLinkType(LinkType linkType) {
        if (linkType == null) {
            typeId = null;
        }
        typeId = linkType.id;
    }

    @JsonIgnore
    public List<Note> getBackwardPeers() {
        return targetNote.linkedNotesOfType(getLinkType().reverseType(), null);
    }

    @JsonIgnore
    public String getExclusiveQuestion() {
        return getLinkType().exclusiveQuestion.replace("not ", "<em>NOT</em> ");
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
