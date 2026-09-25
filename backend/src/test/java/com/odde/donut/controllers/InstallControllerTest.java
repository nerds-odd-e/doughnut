package com.odde.donut.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class InstallControllerTest extends ControllerTestBase {
  @Autowired private MockMvc mockMvc;

  @Test
  void installReturnsBashScriptByDefault() throws Exception {
    mockMvc
        .perform(get("/install"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/plain"))
        .andExpect(content().string(containsString("#!/usr/bin/env bash")))
        .andExpect(content().string(containsString("doughnut-cli-latest/doughnut")))
        .andExpect(content().string(containsString("BASE_URL")));
  }

  @Test
  void installReturnsPowerShellScriptWhenWin32ParamIsTrue() throws Exception {
    mockMvc
        .perform(get("/install").param("win32", "true"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/plain"))
        .andExpect(content().string(containsString("$ErrorActionPreference")))
        .andExpect(content().string(containsString("Invoke-WebRequest")))
        .andExpect(content().string(containsString("doughnut-cli-latest/doughnut")));
  }
}
