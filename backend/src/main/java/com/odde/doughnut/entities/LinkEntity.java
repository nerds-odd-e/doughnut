package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "link")
public class LinkEntity {
    public enum LinkType {
        BELONGS_TO("belongs to"),
        RELATED_TO("is related to"),
        SIMILAR_TO("is similar to"),
        OPPOSITE_OF("is the opposite of"),
        CONFUSE_WITH("confuses with");

        public final String label;

        private LinkType(String label) {
            this.label = label;
        }
    }
    @Id
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
}
