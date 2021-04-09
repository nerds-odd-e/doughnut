package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Image;
import com.odde.doughnut.entities.repositories.ImageBlobRepository;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class ImageControllerTests {
    @Autowired
    ImageBlobRepository imageBlobRepository;

    @Autowired
    MakeMe makeMe;
    ImageController controller;

    @BeforeEach
    void setup() {
        controller = new ImageController(imageBlobRepository);
    }

    @Test
    void contentType() {
        Image image = makeMe.anImage().please();
        makeMe.refresh(image);
        ResponseEntity<byte[]> resp = controller.show(image, "filename");
        assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(resp.getHeaders().getContentType().toString(), equalTo("image/png"));
        assertThat(resp.getHeaders().getContentDisposition().toString(), equalTo("inline; filename=\"example.png\""));
    }
}

