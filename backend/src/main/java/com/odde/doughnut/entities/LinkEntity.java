package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "link")
public class LinkEntity {

    public enum LinkType {
        BELONGS_TO("belongs to"),
        HAS("has"),
        RELATED_TO("is related to"),
        OPPOSITE_OF("is the opposite of"),
        BROUGHT_BY("is brought by"),
        AUTHOR_OF("is author of"),
        SIMILAR_TO("is similar to"),
        CONFUSE_WITH("confuses with");

        public final String label;

        private LinkType(String label) {
            this.label = label;
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

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "source_id", referencedColumnName = "id")
    @Getter @Setter private NoteEntity sourceNote;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "target_id", referencedColumnName = "id")
    @Getter @Setter private NoteEntity targetNote;

    @Column(name = "type")
    @Getter
    @Setter
    private String type;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @Getter @Setter private UserEntity userEntity;

    @Column(name = "created_at")
    @Getter
    @Setter
    private Timestamp createAt = new Timestamp(System.currentTimeMillis());

    public String getQuizDescription() {
        return "`" + getSourceNote().getTitle() + "` " + getType() + ":";
    }

    LinkType getLinkType() {
        return LinkType.fromString(type);
    }
}
