package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Audio;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Transactional
public class NoteAccessoriesDTOTest {

  NoteAccessoriesDTO accessoriesDTO = new NoteAccessoriesDTO();

  @Test
  void validate() throws IOException {
    Audio audio = accessoriesDTO.fetchAttachAudio(null);
    assertNull(audio);
  }
}
