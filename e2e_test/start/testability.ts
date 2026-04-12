/// <reference types="Cypress" />
// @ts-check
import type { Randomization } from '@generated/doughnut-backend-api'
import type { QuestionSuggestionParams } from '@generated/doughnut-backend-api'
import type ServiceMocker from '../support/ServiceMocker'
import type { NoteTestData } from '@generated/doughnut-backend-api'
import type { PredefinedQuestionsTestData } from '@generated/doughnut-backend-api'
import type { TimeTravel } from '@generated/doughnut-backend-api'
import type { TimeTravelRelativeToNow } from '@generated/doughnut-backend-api'
import type { SuggestedQuestionsData } from '@generated/doughnut-backend-api'
import type {
  AttachBookRequestFull,
  NoteRealm,
} from '@generated/doughnut-backend-api'
import type { NotesTestData } from '@generated/doughnut-backend-api'
import {
  AssimilationController,
  NoteController,
  NotebookBooksController,
  TestabilityRestController,
} from '@generated/doughnut-backend-api/sdk.gen'

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

/** Must match page count in `e2e_test/fixtures/book_reading/blank_5_pages.pdf`. */
const BLANK_BOOK_FIXTURE_PAGE_COUNT = 5

function pageCountFromContentList(contentList: Array<unknown>): number {
  let max = -1
  for (const o of contentList) {
    if (o !== null && typeof o === 'object' && 'page_idx' in o) {
      const p = (o as { page_idx: unknown }).page_idx
      if (typeof p === 'number' && Number.isFinite(p)) {
        max = Math.max(max, p)
      }
    }
  }
  if (max < 0) {
    throw new Error('contentList has no valid page_idx')
  }
  return max + 1
}

const testability = () => {
  return {
    cleanDBAndResetTestabilitySettings() {
      return cleanAndReset(cy, 5).then(() =>
        cy.wrap({}).as(injectedNoteIdMapAliasName)
      )
    },

    attachBookToNotebook(
      notebookTitle: string,
      bookName: string,
      contentList: Array<unknown>
    ) {
      expect(
        pageCountFromContentList(contentList),
        `contentList page range must match blank_${BLANK_BOOK_FIXTURE_PAGE_COUNT}_pages.pdf`
      ).to.equal(BLANK_BOOK_FIXTURE_PAGE_COUNT)
      return this.getInjectedNoteIdByTitle(notebookTitle).then((noteId) =>
        cy
          .wrap(NoteController.showNote({ path: { note: noteId } }), {
            log: false,
          })
          .then((response) => {
            const realm = unwrapData<NoteRealm>(response)
            const notebookId = realm.notebook?.id
            expect(notebookId, 'head note must belong to a notebook').to.be.a(
              'number'
            )
            return cy
              .readFile(
                'e2e_test/fixtures/book_reading/blank_5_pages.pdf',
                null
              )
              .then((pdfBuffer) => {
                const pdfBlob = new Blob([pdfBuffer as BlobPart], {
                  type: 'application/pdf',
                })
                const file = new File([pdfBlob], 'blank.pdf', {
                  type: 'application/pdf',
                })
                const metadataBlob = new Blob(
                  [
                    JSON.stringify({
                      bookName,
                      format: 'pdf',
                      contentList,
                    }),
                  ],
                  { type: 'application/json' }
                )
                return cy.wrap(
                  NotebookBooksController.attachBook({
                    path: { notebook: notebookId },
                    body: {
                      metadata:
                        metadataBlob as unknown as AttachBookRequestFull,
                      file,
                    },
                  }),
                  { log: false }
                )
              })
          })
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
    injectRelationship(
      type: string,
      fromNoteTopic: string,
      toNoteTopic: string
    ) {
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

          const promise = TestabilityRestController.createRelationships({
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

    assimilateNote(noteTitle: string) {
      return this.getInjectedNoteIdByTitle(noteTitle).then((noteId) => {
        return cy.wrap(
          AssimilationController.assimilate({
            body: { noteId, skipMemoryTracking: false },
          }),
          { log: false }
        )
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

    setOpenAiTokenOverride(token: string | null) {
      return cy.wrap(
        TestabilityRestController.setOpenAiToken({
          body: { token: token ?? '' },
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
