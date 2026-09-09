/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import { mock_services } from '../start'
import { resolveOpenAiMockEndpointContext } from '../start/mock_services/openAiMockEndpointContext'
import {
  fetchOpenAiMockIsolationProof,
  signalPeerSeededForWorktreeResetIsolation,
  worktreeBrowserIsolationActive,
} from './worktreeOpenAiMockIsolation'

When("I keep the saved note across the other worktree's fixture reset", () => {
  signalPeerSeededForWorktreeResetIsolation()
})

When(
  "I keep the CLI notebook across the other worktree's fixture reset",
  () => {
    signalPeerSeededForWorktreeResetIsolation()
  }
)

When(
  "I keep my OpenAI mock configured across the other worktree's mock reset",
  () => {
    signalPeerSeededForWorktreeResetIsolation()
  }
)

Then('my OpenAI mock still records only my request marker', () => {
  fetchOpenAiMockIsolationProof().then((params) => {
    const marker = params?.requestMarker ?? 'Please complete the note content.'
    const endpoint = resolveOpenAiMockEndpointContext()
    if (worktreeBrowserIsolationActive()) {
      expect(
        endpoint.servingPort,
        'isolated OpenAI mock must not use shared serving port 5001'
      ).to.not.equal(5001)
      expect(
        endpoint.managementUrl,
        'isolated OpenAI mock must not use shared management :2525'
      ).to.not.include(':2525')
    }
    cy.log(
      `Recording check via ${endpoint.managementUrl} port ${endpoint.servingPort}`
    )
    mock_services.openAi().expectLastResponsesPostBodyContains(marker)
  })
})
