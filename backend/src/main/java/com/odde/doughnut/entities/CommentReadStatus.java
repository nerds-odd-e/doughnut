package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "comment_read_status")
public class CommentReadStatus {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "comment_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private Comment comment;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private User user;

    @Column(name = "created_at")
    @Getter
    @Setter
    private Timestamp createdAt;

    @Column(name = "updated_at")
    @Setter
    @Getter
    private Timestamp updatedAt;

    @Column(name = "deleted_at")
    @Setter
    @Getter
    private Timestamp deletedAt;

}
