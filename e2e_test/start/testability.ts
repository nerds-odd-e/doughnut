/// <reference types="Cypress" />
// @ts-check
import type { Randomization } from '@generated/backend'
import type { QuestionSuggestionParams } from '@generated/backend'
import type ServiceMocker from '../support/ServiceMocker'
import type { NoteTestData } from '@generated/backend'
import type { PredefinedQuestionsTestData } from '@generated/backend'
import type { TimeTravel } from '@generated/backend'
import type { TimeTravelRelativeToNow } from '@generated/backend'
import type { SuggestedQuestionsData } from '@generated/backend'
import type { NotesTestData } from '@generated/backend'
import { TestabilityRestController } from '@generated/backend/sdk.gen'

const hourOfDay = (days: number, hours: number) => {
  return new Date(1976, 5, 1 + days, hours)
}

const cleanAndReset = (cy: Cypress.cy & CyEventEmitter, countdown: number) => {
  return cy
    .wrap(TestabilityRestController.resetDbAndTestabilitySettings(), {
      log: false,
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

const injectedNoteIdMapAliasName = 'injectedNoteIdMap'

const unwrapData = <T>(result: T | { data: T } | undefined): T => {
  if (result && typeof result === 'object' && 'data' in result) {
    return (result as { data: T }).data
  }
  return result as T
}

const testability = () => {
  return {
    cleanDBAndResetTestabilitySettings() {
      return cleanAndReset(cy, 5).then(() =>
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

    injectNotes(
      noteTestData: NoteTestData[],
      externalIdentifier: string,
      circleName: string | null = null
    ) {
      const requestBody: NotesTestData = {
        externalIdentifier,
        circleName: circleName ?? undefined,
        noteTestData,
      }

      return cy.get(`@${injectedNoteIdMapAliasName}`).then((existingMap) => {
        return cy
          .wrap(
            TestabilityRestController.injectNotes({
              body: requestBody,
            }).then((response) => {
              const data = unwrapData<Record<string, number>>(response)
              const expectedCount = noteTestData.length
              const actualCount = Object.keys(data).length
              expect(
                actualCount,
                `Expected ${expectedCount} notes to be created, but backend only created ${actualCount} notes`
              ).to.equal(expectedCount)
              return { ...existingMap, ...data }
            }),
            { log: false }
          )
          .as(injectedNoteIdMapAliasName)
      })
    },
    injectNumberNotes(
      notebook: string,
      numberOfNotes: number,
      creatorId: string
    ) {
      const notes: Record<string, string>[] = [
        { Title: notebook },
        ...new Array(numberOfNotes).fill(0).map((_, index) => ({
          Title: `Note about ${index}`,
          'Parent Title': notebook,
        })),
      ]
      return this.injectNotes(notes, creatorId)
    },
    injectPredefinedQuestionsToNotebook(
      predefinedQuestionsTestData: PredefinedQuestionsTestData
    ) {
      return cy
        .wrap(
          TestabilityRestController.injectPredefinedQuestion({
            body: predefinedQuestionsTestData,
          }),
          { log: false }
        )
        .then((response) => {
          const data = unwrapData<Record<string, unknown>>(response)
          expect(Object.keys(data).length).to.equal(
            predefinedQuestionsTestData.predefinedQuestionTestData?.length
          )
        })
    },
    injectYesNoQuestionsForNumberNotes(
      notebook: string,
      numberOfNotes: number,
      notebookCertifiable?: boolean
    ) {
      const predefinedQuestion: Record<string, string>[] = new Array(
        numberOfNotes
      )
        .fill(0)
        .map((_, index) => ({
          'Note Title': `Note about ${index}`,
          Question: `Is ${index} * ${index} = ${index * index}?`,
          Answer: 'Yes',
          'One Wrong Choice': 'No',
          Approved: 'true',
        }))
      return this.injectPredefinedQuestionsToNotebook({
        notebookTitle: notebook,
        notebookCertifiable,
        predefinedQuestionTestData: predefinedQuestion,
      })
    },
    injectNumbersNotebookWithQuestions(
      notebook: string,
      numberOfQuestion: number,
      creatorId: string,
      notebookCertifiable?: boolean
    ) {
      return this.injectNumberNotes(notebook, numberOfQuestion, creatorId)
        .then(() =>
          this.injectYesNoQuestionsForNumberNotes(
            notebook,
            numberOfQuestion,
            notebookCertifiable
          )
        )
        .then(() => this.shareToBazaar(notebook))
    },
    injectLink(type: string, fromNoteTopic: string, toNoteTopic: string) {
      return cy
        .get(`@${injectedNoteIdMapAliasName}`)
        .then((injectedNoteIdMap) => {
          expect(injectedNoteIdMap).to.have.property(fromNoteTopic)
          expect(injectedNoteIdMap).to.have.property(toNoteTopic)
          const fromNoteId = injectedNoteIdMap[fromNoteTopic]
          const toNoteId = injectedNoteIdMap[toNoteTopic]

          const requestBody = {
            type,
            source_id: fromNoteId.toString(),
            target_id: toNoteId.toString(),
          }

          const promise = TestabilityRestController.linkNotes({
            body: requestBody,
          })
          return cy.wrap(promise, { log: false })
        })
    },

    injectSuggestedQuestions(examples: Array<QuestionSuggestionParams>) {
      return cy.get<string>('@currentLoginUser').then((username) => {
        const requestBody: SuggestedQuestionsData = { examples, username }
        const promise = TestabilityRestController.injectSuggestedQuestion({
          body: requestBody,
        })
        return cy.wrap(promise, { log: false })
      })
    },

    injectSuggestedQuestion(questionStem: string, positiveFeedback: boolean) {
      this.injectSuggestedQuestions([
        {
          positiveFeedback,
          preservedNoteContent: 'note content',
          realCorrectAnswers: '',
          preservedQuestion: {
            f0__multipleChoicesQuestion: {
              stem: questionStem,
              choices: ['choice 1', 'choice 2'],
            },
            f1__correctChoiceIndex: 0,
            f2__strictChoiceOrder: true,
          },
        },
      ])
    },

    getInjectedNoteIdByTitle(noteTopology: string) {
      return cy
        .get(`@${injectedNoteIdMapAliasName}`)
        .then((injectedNoteIdMap) => {
          expect(
            injectedNoteIdMap,
            `"${noteTopology}" is not in the injected note. Did you created during the test?`
          ).to.have.property(noteTopology)
          return injectedNoteIdMap[noteTopology]
        })
    },

    timeTravelTo(day: number, hour: number) {
      this.backendTimeTravelTo(day, hour)
      cy.window().then((window) => {
        cy.tick(hourOfDay(day, hour).getTime() - new window.Date().getTime())
      })
    },

    backendTimeTravelToDate(date: Date) {
      const requestBody: TimeTravel = { travel_to: JSON.stringify(date) }

      return cy.wrap(
        TestabilityRestController.timeTravel({ body: requestBody }),
        { log: false }
      )
    },

    backendTimeTravelTo(day: number, hour: number) {
      const requestBody: TimeTravel = {
        travel_to: JSON.stringify(hourOfDay(day, hour)),
      }

      return cy.wrap(
        TestabilityRestController.timeTravel({ body: requestBody }),
        { log: false }
      )
    },

    backendTimeTravelRelativeToNow(hours: number) {
      const requestBody: TimeTravelRelativeToNow = {
        hours,
      }

      return cy.wrap(
        TestabilityRestController.timeTravelRelativeToNow({
          body: requestBody,
        }),
        { log: false }
      )
    },

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

    shareToBazaar(noteTopology: string) {
      const requestBody = { noteTopology }

      return cy.wrap(
        TestabilityRestController.shareToBazaar({
          body: requestBody,
        }),
        { log: false }
      )
    },

    injectCircle(circleInfo: Record<string, string>) {
      return cy.wrap(
        TestabilityRestController.injectCircle({ body: circleInfo }),
        {
          log: false,
        }
      )
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
    mockBrowserTime() {
      //
      // when using `cy.clock()` to set the time,
      // for Vue component with v-if for a ref/react object that is changed during mount by async call
      // the event, eg. click, will not work.
      //
      cy.clock(hourOfDay(0, 0), [
        'setTimeout',
        'setInterval',
        'clearInterval',
        'clearTimeout',
        'Date',
      ])
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
