package com.odde.doughnut.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "image")
public class Image extends Attachment {}
