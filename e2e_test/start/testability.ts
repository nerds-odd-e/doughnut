/// <reference types="Cypress" />
// @ts-check
import type { Randomization } from '@generated/donut-backend-api'
import type ServiceMocker from '../support/ServiceMocker'
import type { McqsTestData } from '@generated/donut-backend-api'
import { TestabilityRestController } from '@generated/donut-backend-api/sdk.gen'
import { circleIdAlias } from './pageObjects/circlePage'
import { assimilateTestabilityMethods } from './testabilityAssimilate'
import { bazaarTestabilityMethods } from './testabilityBazaar'
import { bookTestabilityMethods } from './testabilityBook'
import { notebookGitTestabilityMethods } from './testabilityNotebookGit'
import { notebookStructureTestabilityMethods } from './testabilityNotebookStructure'
import {
  injectedNoteIdMapAliasName,
  noteTestabilityMethods,
} from './testabilityNote'
import { relationshipTestabilityMethods } from './testabilityRelationship'
import { conversationTestabilityMethods } from './testabilityConversation'
import { recallTestabilityMethods } from './testabilityRecall'
import { timeTravelTestabilityMethods } from './testabilityTimeTravel'
import { unwrapData } from './unwrapApi'

/** Per-attempt budget for DB truncate + seed; CI under load often exceeds 6s. */
const CLEAN_DB_REQUEST_TIMEOUT_MS = 30_000

const cleanAndReset = (cy: Cypress.cy & CyEventEmitter, countdown: number) => {
  // Abort so a hung fetch rejects (retries run). cy.wrap alone only times out
  // the command and never reaches the rejection handler below.
  const signal = AbortSignal.timeout(CLEAN_DB_REQUEST_TIMEOUT_MS)
  return cy
    .wrap(TestabilityRestController.resetDbAndTestabilitySettings({ signal }), {
      log: false,
      timeout: CLEAN_DB_REQUEST_TIMEOUT_MS + 1_000,
    })
    .then(
      () => {
        // Success
      },
      (error) => {
        if (countdown > 0) {
          return cleanAndReset(cy, countdown - 1)
        } else {
          throw error
        }
      }
    )
}

const testability = () => {
  return {
    cleanDBAndResetTestabilitySettings() {
      return cleanAndReset(cy, 2).then(() =>
        cy.wrap({}).as(injectedNoteIdMapAliasName)
      )
    },

    featureToggle(enabled: boolean) {
      return cy.wrap(
        TestabilityRestController.enableFeatureToggle({
          body: { enabled: enabled.toString() },
        }),
        { log: false }
      )
    },

    ...noteTestabilityMethods,

    injectMcqsToNotebook(mcqsTestData: McqsTestData) {
      return cy
        .wrap(
          TestabilityRestController.injectMcq({
            body: mcqsTestData,
          }),
          { log: false }
        )
        .then((response) => {
          const data = unwrapData<Record<string, unknown>>(response)
          expect(Object.keys(data).length).to.equal(
            mcqsTestData.mcqTestData?.length
          )
        })
    },
    injectYesNoQuestionsForNumberNotes(
      notebook: string,
      numberOfNotes: number
    ) {
      const mcq: Record<string, string>[] = new Array(numberOfNotes)
        .fill(0)
        .map((_, index) => ({
          'Note Title': `Note about ${index}`,
          Question: `Is ${index} * ${index} = ${index * index}?`,
          Answer: 'Yes',
          'One Wrong Choice': 'No',
        }))
      return this.injectMcqsToNotebook({
        notebookName: notebook,
        mcqTestData: mcq,
      })
    },
    injectNumbersNotebookWithQuestions(
      notebook: string,
      numberOfQuestion: number,
      creatorId: string
    ) {
      return this.injectNumberNotes(notebook, numberOfQuestion, creatorId)
        .then(() =>
          this.injectYesNoQuestionsForNumberNotes(notebook, numberOfQuestion)
        )
        .then(() => this.shareToBazaar(notebook))
    },
    ...relationshipTestabilityMethods,

    ...assimilateTestabilityMethods,
    ...bazaarTestabilityMethods,
    ...bookTestabilityMethods,
    ...notebookGitTestabilityMethods,
    ...notebookStructureTestabilityMethods,
    ...recallTestabilityMethods,
    ...timeTravelTestabilityMethods,

    ...conversationTestabilityMethods,

    randomizerSettings(strategy: 'first' | 'last' | 'seed', seed: number) {
      const requestBody: Randomization = { choose: strategy, seed }

      return cy.wrap(
        TestabilityRestController.randomizer({ body: requestBody }),
        { log: false }
      )
    },

    triggerException() {
      return cy.wrap(
        TestabilityRestController.triggerException().catch(() => {
          // Expected: the exception triggers a 500 error which is caught and logged
          return Promise.resolve()
        }),
        { log: false }
      )
    },

    injectCircle(circleInfo: Record<string, string>) {
      const circleName = circleInfo.circleName
      return cy
        .wrap(TestabilityRestController.injectCircle({ body: circleInfo }), {
          log: false,
        })
        .then((response) => {
          const [circleId, invitationCode] = String(unwrapData(response)).split(
            ',',
            2
          )
          if (
            !(
              circleName &&
              circleId &&
              /^\d+$/.test(circleId) &&
              invitationCode
            )
          ) {
            throw new Error(
              `inject_circle did not return id and invitation code for "${circleName}"`
            )
          }
          return cy
            .wrap(circleId)
            .as(circleIdAlias(circleName))
            .then(() => cy.wrap(invitationCode).as('circleInvitationCode'))
        })
    },

    updateCurrentUserSettingsWith(hash: Record<string, string>) {
      return cy.get<string>('@currentLoginUser').then((username) => {
        const promise = TestabilityRestController.testabilityUpdateUser({
          query: { username },
          body: hash,
        })
        return cy.wrap(promise, { log: false })
      })
    },

    setServiceUrl(serviceName: string, serviceUrl: string) {
      const requestBody = { [serviceName]: serviceUrl }

      return cy.wrap(
        TestabilityRestController.replaceServiceUrl({
          body: requestBody,
        }),
        { log: false }
      )
    },

    setOpenAiTokenOverride(token: string | null) {
      return cy.wrap(
        TestabilityRestController.setOpenAiToken({
          body: { token: token ?? '' },
        }),
        { log: false }
      )
    },
    mockService(serviceMocker: ServiceMocker) {
      this.setServiceUrl(serviceMocker.serviceName, serviceMocker.serviceUrl)
      serviceMocker.install()
    },

    restoreMockedService(serviceMocker: ServiceMocker) {
      this.setServiceUrl(serviceMocker.serviceName, '')
    },
  }
}

export default testability
