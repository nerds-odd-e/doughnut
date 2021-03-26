package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "link")
public class LinkEntity {

    public enum LinkType {
        BELONGS_TO("belongs to", "does not belong to"),
        HAS("has", "does not have"),
        RELATED_TO("is related to", "is not related to"),
        OPPOSITE_OF("is the opposite of", "is not the opposite of"),
        BROUGHT_BY("is brought by", "is not brought by"),
        AUTHOR_OF("is author of", "is not author of"),
        SIMILAR_TO("is similar to", "is not simlilr to"),
        CONFUSE_WITH("confuses with", "does not confuse with");

        public final String label;
        public final String exclusiveQuestion;

        private LinkType(String label, String exclusiveQuestion) {
            this.label = label;
            this.exclusiveQuestion = exclusiveQuestion;
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
            return this;
        }

    }

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "source_id", referencedColumnName = "id")
    @Getter @Setter private NoteEntity sourceNote;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "target_id", referencedColumnName = "id")
    @Getter @Setter private NoteEntity targetNote;

    @Column(name = "type")
    @Getter
    @Setter
    private String type;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @Getter @Setter private UserEntity userEntity;

    @Column(name = "created_at")
    @Getter
    @Setter
    private Timestamp createAt = new Timestamp(System.currentTimeMillis());

    @OneToMany(mappedBy = "linkEntity", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    private final List<ReviewPointEntity> reviewPointEntities = new ArrayList<>();

    public String getQuizDescription() {
        return "`" + getSourceNote().getTitle() + "` " + getType() + ":";
    }

    LinkType getLinkType() {
        return LinkType.fromString(type);
    }
}
