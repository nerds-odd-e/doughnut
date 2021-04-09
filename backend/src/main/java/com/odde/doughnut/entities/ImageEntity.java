package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "image")
public class ImageEntity {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull

    @Size(min = 1, max = 255) @Getter @Setter
    private String name;
    @Getter @Setter private String type;
    @Column(name="storage_type")
    @Getter @Setter private String storageType;

    @Column(name= "image_blob_id", insertable = false, updatable = false)
    @Getter
    private Integer imageBlobId;

    @OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "image_blob_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private ImageBlob imageBlob;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private UserEntity userEntity;
}
