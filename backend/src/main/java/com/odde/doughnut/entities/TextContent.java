package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.sql.Timestamp;

@Entity
@Table(name = "text_content")
public class TextContent {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Size(min = 1, max = 100)
    @Getter
    @Setter
    private String title = "";

    @Getter
    @Setter
    private String description;

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

