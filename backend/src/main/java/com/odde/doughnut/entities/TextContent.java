package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "text_content")
public class TextContent {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Getter
    @Setter
    private String title = "";

    @Getter
    @Setter
    private String description;

    @Getter
    @Setter
    private String language;

    @Column(name = "updated_at")
    @Getter
    @Setter
    private Timestamp updatedAt;

    public void updateTextContent(TextContent textContent, Timestamp currentUTCTimestamp) {
        setUpdatedAt(currentUTCTimestamp);
        setTitle(textContent.getTitle());
        setDescription(textContent.getDescription());
    }
}

