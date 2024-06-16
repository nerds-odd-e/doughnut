package com.odde.doughnut.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ImageWithMask {
  @Column(name = "image_url")
  public String noteImage;

  @Column(name = "image_mask")
  public String imageMask;
}
