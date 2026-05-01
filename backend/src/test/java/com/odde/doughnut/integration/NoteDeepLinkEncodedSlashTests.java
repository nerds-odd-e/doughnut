package com.odde.doughnut.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NoteDeepLinkEncodedSlashTests {

  @Autowired private MockMvc mockMvc;

  @Test
  void deepLinkWithEncodedSlashInSlugSegmentIsNotRejectedByFirewall() throws Exception {
    // Use a fully-built URI so %2F is not normalized by get(String) (matches real container).
    mockMvc
        .perform(get(URI.create("http://localhost/d/notebooks/1/notes/outer%2Finner")))
        .andExpect(status().isOk());
  }
}
