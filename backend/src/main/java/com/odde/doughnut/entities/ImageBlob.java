package com.odde.doughnut.entities;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "image_blob")
public class ImageBlob {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Lob @Getter @Setter private byte[] data;
}
