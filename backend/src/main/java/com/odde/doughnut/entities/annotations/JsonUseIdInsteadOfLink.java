package com.odde.doughnut.entities.annotations;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.services.EntityIdResolver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME) // IMPORTANT
@JacksonAnnotationsInside
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        resolver = EntityIdResolver.class,
        scope = Link.class,
        property = "id")
@JsonIdentityReference(alwaysAsId = true)
public @interface JsonUseIdInsteadOfLink {
}
