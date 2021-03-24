package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.CircleEntity;
import com.odde.doughnut.entities.CircleJoiningByInvitationEntity;
import com.odde.doughnut.entities.ImageEntity;
import com.odde.doughnut.entities.repositories.ImageBlobRepository;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
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
        ImageEntity imageEntity = makeMe.anImage().please();
        makeMe.refresh(imageEntity);
        ResponseEntity<byte[]> resp = controller.show(imageEntity, "filename");
        assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(resp.getHeaders().getContentType().toString(), equalTo("image/png"));
        assertThat(resp.getHeaders().getContentDisposition().toString(), equalTo("inline; filename=\"example.png\""));
    }
}

