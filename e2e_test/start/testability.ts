/// <reference types="Cypress" />
// @ts-check
import type { Randomization } from '@generated/backend/models/Randomization'
import type { QuestionSuggestionParams } from '@generated/backend/models/QuestionSuggestionParams'
import type ServiceMocker from '../support/ServiceMocker'
import type { NoteTestData } from '@generated/backend/models/NoteTestData'
import type { PredefinedQuestionsTestData } from '@generated/backend/models/PredefinedQuestionsTestData'
import type { TimeTravel } from '@generated/backend/models/TimeTravel'
import type { TimeTravelRelativeToNow } from '@generated/backend/models/TimeTravelRelativeToNow'
import type { SuggestedQuestionsData } from '@generated/backend/models/SuggestedQuestionsData'
import type { NotesTestData } from '@generated/backend/models/NotesTestData'
import { TestabilityRestControllerService } from '@generated/backend/services/TestabilityRestControllerService'
import { extractRequestConfig } from './utils/apiConfigExtractor'

const hourOfDay = (days: number, hours: number) => {
  return new Date(1976, 5, 1 + days, hours)
}

const cleanAndReset = (cy: Cypress.cy & CyEventEmitter, countdown: number) => {
  const config = extractRequestConfig((httpRequest) => {
    const service = new TestabilityRestControllerService(httpRequest)
    return service.resetDbAndTestabilitySettings()
  })

  cy.request({
    method: config.method,
    url: config.url,
    failOnStatusCode: countdown === 1,
  }).then((response) => {
    if (countdown > 0 && response.status !== 200) {
      cleanAndReset(cy, countdown - 1)
    }
  })
}

const injectedNoteIdMapAliasName = 'injectedNoteIdMap'

const testability = () => {
  return {
    cleanDBAndResetTestabilitySettings() {
      cleanAndReset(cy, 5)
      cy.wrap({}).as(injectedNoteIdMapAliasName)
    },

    featureToggle(enabled: boolean) {
      const config = extractRequestConfig((httpRequest) => {
        const service = new TestabilityRestControllerService(httpRequest)
        return service.enableFeatureToggle({ enabled: enabled.toString() })
      })

      cy.request({
        method: config.method,
        url: config.url,
        body: { enabled: enabled.toString() },
        failOnStatusCode: true,
      })
    },

    injectNotes(
      noteTestData: NoteTestData[],
      externalIdentifier = '',
      circleName: string | null = null
    ) {
      const requestBody: NotesTestData = {
        externalIdentifier,
        circleName,
        noteTestData,
      }

      const config = extractRequestConfig((httpRequest) => {
        const service = new TestabilityRestControllerService(httpRequest)
        return service.injectNotes(requestBody)
      })

      return cy
        .request({
          method: config.method,
          url: config.url,
          body: requestBody,
        })
        .then((response) => {
          const expectedCount = noteTestData.length
          const actualCount = Object.keys(response.body).length
          expect(
            actualCount,
            `Expected ${expectedCount} notes to be created, but backend only created ${actualCount} notes`
          ).to.equal(expectedCount)
          cy.get(`@${injectedNoteIdMapAliasName}`).then((existingMap) => {
            const mergedMap = { ...existingMap, ...response.body }
            cy.wrap(mergedMap).as(injectedNoteIdMapAliasName)
          })
        })
    },
    injectNumberNotes(
      notebook: string,
      numberOfNotes: number,
      creatorId?: string
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
      const config = extractRequestConfig((httpRequest) => {
        const service = new TestabilityRestControllerService(httpRequest)
        return service.injectPredefinedQuestion(predefinedQuestionsTestData)
      })

      cy.request({
        method: config.method,
        url: config.url,
        body: predefinedQuestionsTestData,
      }).then((response) => {
        expect(Object.keys(response.body).length).to.equal(
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
      creatorId?: string,
      notebookCertifiable?: boolean
    ) {
      this.injectNumberNotes(notebook, numberOfQuestion, creatorId)
      this.injectYesNoQuestionsForNumberNotes(
        notebook,
        numberOfQuestion,
        notebookCertifiable
      )
      this.shareToBazaar(notebook)
    },
    injectLink(type: string, fromNoteTopic: string, toNoteTopic: string) {
      cy.get(`@${injectedNoteIdMapAliasName}`).then((injectedNoteIdMap) => {
        expect(injectedNoteIdMap).haveOwnPropertyDescriptor(fromNoteTopic)
        expect(injectedNoteIdMap).haveOwnPropertyDescriptor(toNoteTopic)
        const fromNoteId = injectedNoteIdMap[fromNoteTopic]
        const toNoteId = injectedNoteIdMap[toNoteTopic]

        const requestBody = {
          type,
          source_id: fromNoteId.toString(),
          target_id: toNoteId.toString(),
        }

        const config = extractRequestConfig((httpRequest) => {
          const service = new TestabilityRestControllerService(httpRequest)
          return service.linkNotes(requestBody)
        })

        cy.request({
          method: config.method,
          url: config.url,
          body: requestBody,
          failOnStatusCode: true,
        })
      })
    },

    injectSuggestedQuestions(examples: Array<QuestionSuggestionParams>) {
      const requestBody: SuggestedQuestionsData = { examples }

      const config = extractRequestConfig((httpRequest) => {
        const service = new TestabilityRestControllerService(httpRequest)
        return service.injectSuggestedQuestion(requestBody)
      })

      cy.request({
        method: config.method,
        url: config.url,
        body: requestBody,
        failOnStatusCode: true,
      })
    },

    injectSuggestedQuestion(questionStem: string, positiveFeedback: boolean) {
      this.injectSuggestedQuestions([
        {
          positiveFeedback,
          preservedNoteContent: 'note content',
          realCorrectAnswers: '',
          preservedQuestion: {
            multipleChoicesQuestion: {
              stem: questionStem,
              choices: ['choice 1', 'choice 2'],
            },
            correctChoiceIndex: 0,
            strictChoiceOrder: true,
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
          ).haveOwnPropertyDescriptor(noteTopology)
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

      const config = extractRequestConfig((httpRequest) => {
        const service = new TestabilityRestControllerService(httpRequest)
        return service.timeTravel(requestBody)
      })

      cy.request({
        method: config.method,
        url: config.url,
        body: requestBody,
        failOnStatusCode: true,
      })
    },

    backendTimeTravelTo(day: number, hour: number) {
      const requestBody: TimeTravel = {
        travel_to: JSON.stringify(hourOfDay(day, hour)),
      }

      const config = extractRequestConfig((httpRequest) => {
        const service = new TestabilityRestControllerService(httpRequest)
        return service.timeTravel(requestBody)
      })

      cy.request({
        method: config.method,
        url: config.url,
        body: requestBody,
        failOnStatusCode: true,
      })
    },

    backendTimeTravelRelativeToNow(hours: number) {
      const requestBody: TimeTravelRelativeToNow = {
        hours: JSON.stringify(hours),
      }

      const config = extractRequestConfig((httpRequest) => {
        const service = new TestabilityRestControllerService(httpRequest)
        return service.timeTravelRelativeToNow(requestBody)
      })

      return cy.request({
        method: config.method,
        url: config.url,
        body: requestBody,
        failOnStatusCode: true,
      })
    },

    randomizerSettings(strategy: 'first' | 'last' | 'seed', seed: number) {
      const requestBody: Randomization = { choose: strategy, seed }

      const config = extractRequestConfig((httpRequest) => {
        const service = new TestabilityRestControllerService(httpRequest)
        return service.randomizer(requestBody)
      })

      cy.request({
        method: config.method,
        url: config.url,
        body: requestBody,
        failOnStatusCode: true,
      })
    },

    triggerException() {
      const config = extractRequestConfig((httpRequest) => {
        const service = new TestabilityRestControllerService(httpRequest)
        return service.triggerException()
      })

      cy.request({
        method: config.method,
        url: config.url,
        failOnStatusCode: false,
      })
    },

    shareToBazaar(noteTopology: string) {
      const requestBody = { noteTopology }

      const config = extractRequestConfig((httpRequest) => {
        const service = new TestabilityRestControllerService(httpRequest)
        return service.shareToBazaar(requestBody)
      })

      cy.request({
        method: config.method,
        url: config.url,
        body: requestBody,
        failOnStatusCode: true,
      })
    },

    injectCircle(circleInfo: Record<string, string>) {
      const config = extractRequestConfig((httpRequest) => {
        const service = new TestabilityRestControllerService(httpRequest)
        return service.injectCircle(circleInfo)
      })

      cy.request({
        method: config.method,
        url: config.url,
        body: circleInfo,
        failOnStatusCode: true,
      })
    },

    updateCurrentUserSettingsWith(hash: Record<string, string>) {
      const config = extractRequestConfig((httpRequest) => {
        const service = new TestabilityRestControllerService(httpRequest)
        return service.updateCurrentUser(hash)
      })

      cy.request({
        method: config.method,
        url: config.url,
        body: hash,
        failOnStatusCode: true,
      })
    },

    setServiceUrl(serviceName: string, serviceUrl: string) {
      const requestBody = { [serviceName]: serviceUrl }

      const config = extractRequestConfig((httpRequest) => {
        const service = new TestabilityRestControllerService(httpRequest)
        return service.replaceServiceUrl(requestBody)
      })

      return cy
        .request({
          method: config.method,
          url: config.url,
          body: requestBody,
        })
        .then((response) => {
          expect(response.status).to.equal(200)
        })
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
      this.setServiceUrl(
        serviceMocker.serviceName,
        serviceMocker.serviceUrl
      ).as(serviceMocker.savedServiceUrlName)
      serviceMocker.install()
    },

    restoreMockedService(serviceMocker: ServiceMocker) {
      this.setServiceUrl(serviceMocker.serviceName, '')
    },
  }
}

export default testability
