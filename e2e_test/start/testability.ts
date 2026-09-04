/// <reference types="Cypress" />
// @ts-check
import type { Randomization } from '@generated/donut-backend-api'
import type ServiceMocker from '../support/ServiceMocker'
import type { NoteTestData } from '@generated/donut-backend-api'
import type { McqsTestData } from '@generated/donut-backend-api'
import type {
  AttachBookRequestFull,
  Folder,
  FolderCreationRequest,
  NoteRealm,
  NotebooksViewedByUser,
} from '@generated/donut-backend-api'
import type { NotesTestDataWritable } from '@generated/donut-backend-api'
import {
  ConversationMessageController,
  NoteController,
  NotebookBooksController,
  NotebookController,
  TestabilityRestController,
  TextContentController,
} from '@generated/donut-backend-api/sdk.gen'
import { circleIdAlias } from './pageObjects/circlePage'
import { noteIdFromUrl } from './noteIdFromUrl'
import { assimilateTestabilityMethods } from './testabilityAssimilate'
import { bazaarTestabilityMethods } from './testabilityBazaar'
import { notebookGitTestabilityMethods } from './testabilityNotebookGit'
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

const injectedNoteIdMapAliasName = 'injectedNoteIdMap'

const conversationIdAlias = (noteTitle: string) =>
  `conversationId_${noteTitle.replace(/\s+/g, '_')}`

/** Same kebab-case rule as relation type options / app compose. */
function relationKebabFromLabel(label: string): string {
  const t = label.trim()
  if (!t) {
    return relationKebabFromLabel('related to')
  }
  return t.toLowerCase().replace(/\s+/g, '-')
}

/** Same title shape as relationship notes created in the app (150 char cap). */
function relationshipNoteTitle(
  sourceTitle: string,
  relationLabel: string,
  targetTitle: string
): string {
  const UNTITLED = 'Untitled'
  const RELATED_TO_LABEL = 'related to'
  const seg = (s: string, fallback: string) => {
    const x = s.trim()
    return x === '' ? fallback : x
  }
  const source = seg(sourceTitle, UNTITLED)
  const relation = seg(relationLabel, RELATED_TO_LABEL)
  const target = seg(targetTitle, UNTITLED)
  const composed = `${source} ${relation} ${target}`.trim()
  if (composed === '') {
    return UNTITLED
  }
  const MAX_TITLE_LENGTH = 150
  if (composed.length > MAX_TITLE_LENGTH) {
    return composed.slice(0, MAX_TITLE_LENGTH)
  }
  return composed
}

/** Same markdown shape as new relationship notes from the app (frontmatter plus optional body). */
function relationshipNoteMarkdown(
  relationLabel: string,
  sourceTitle: string,
  targetTitle: string,
  bodySuffix?: string
): string {
  const relationKebab = relationKebabFromLabel(relationLabel)
  const UNTITLED = 'Untitled'
  const sourceDisplay =
    sourceTitle.trim() === '' ? UNTITLED : sourceTitle.trim()
  const targetDisplay =
    targetTitle.trim() === '' ? UNTITLED : targetTitle.trim()
  const sourceLink = `[[${sourceDisplay}]]`
  const targetLink = `[[${targetDisplay}]]`
  const yamlEscape = (s: string) =>
    s.replace(/\\/g, '\\\\').replace(/"/g, '\\"')
  const suffix = bodySuffix?.trim() ? `\n${bodySuffix.trim()}\n` : ''
  return `---
type: Relationship
relation: ${relationKebab}
source: "${yamlEscape(sourceLink)}"
target: "${yamlEscape(targetLink)}"
---
${suffix}`
}

/** Must match page count in `e2e_test/fixtures/book_reading/blank_5_pages.pdf`. */
const BLANK_BOOK_FIXTURE_PAGE_COUNT = 5

/** Cached once per Cypress process — blank PDF bytes for attachBook. */
let blankBookPdfBuffer: ArrayBuffer | undefined

function readBlankBookPdf() {
  if (blankBookPdfBuffer !== undefined) {
    return cy.wrap(blankBookPdfBuffer, { log: false })
  }
  return cy
    .readFile('e2e_test/fixtures/book_reading/blank_5_pages.pdf', null)
    .then((pdfBuffer) => {
      blankBookPdfBuffer = pdfBuffer as ArrayBuffer
      return blankBookPdfBuffer
    })
}

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
      return cleanAndReset(cy, 2).then(() =>
        cy.wrap({}).as(injectedNoteIdMapAliasName)
      )
    },

    attachBookToNotebook(
      notebookName: string,
      bookName: string,
      contentList: Array<unknown>
    ) {
      expect(
        pageCountFromContentList(contentList),
        `contentList page range must match blank_${BLANK_BOOK_FIXTURE_PAGE_COUNT}_pages.pdf`
      ).to.equal(BLANK_BOOK_FIXTURE_PAGE_COUNT)
      return this.getInjectedNoteIdByTitle(notebookName).then((noteId) =>
        cy
          .wrap(NoteController.showNote({ path: { note: noteId } }), {
            log: false,
          })
          .then((response) => {
            const realm = unwrapData<NoteRealm>(response)
            const notebookId = realm.notebookRealm.notebook.id
            expect(notebookId, 'note must belong to a notebook').to.be.a(
              'number'
            )
            return readBlankBookPdf().then((pdfBuffer) => {
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
                    metadata: metadataBlob as unknown as AttachBookRequestFull,
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
      notebookName: string,
      circleName: string | null = null
    ) {
      const requestBody: NotesTestDataWritable = {
        externalIdentifier,
        notebookName,
        circleName: circleName ?? undefined,
        noteTestData,
      }

      return cy
        .wrap(externalIdentifier)
        .as('injectNotesExternalIdentifier')
        .then(() =>
          cy.get(`@${injectedNoteIdMapAliasName}`).then((existingMap) => {
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
        )
    },

    injectNoteWithContent(
      noteTitle: string,
      content: string,
      externalIdentifier: string,
      notebookName: string,
      folder?: string
    ) {
      const note: NoteTestData = { Title: noteTitle, Content: content }
      if (folder !== undefined) {
        note.Folder = folder
      }
      return this.injectNotes([note], externalIdentifier, notebookName)
    },

    rememberUiCreatedNote(noteTitle: string) {
      return cy.get(`@${injectedNoteIdMapAliasName}`).then((existingMap) => {
        return cy.url().then((url) => {
          const m =
            url.match(/\/n(\d+)/) ??
            url.match(/\/n\/(\d+)/) ??
            url.match(/\/d\/n\/(\d+)/)
          expect(
            m,
            `could not parse note id from URL (expected /n<id>, /n/<id>, or legacy /d/n/<id>): ${url}`
          ).to.not.be.null
          const noteId = Number(m![1])
          const map = existingMap as Record<string, number>
          return cy
            .wrap({ ...map, [noteTitle]: noteId })
            .as(injectedNoteIdMapAliasName)
        })
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
          Folder: notebook,
        })),
      ]
      return this.injectNotes(notes, creatorId, notebook)
    },
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
    injectRelationshipNote(
      notebookName: string,
      relationTypeLabel: string,
      fromNoteTopic: string,
      toNoteTopic: string,
      bodySuffix?: string
    ) {
      return cy
        .get<string>('@injectNotesExternalIdentifier')
        .then((externalIdentifier) => {
          const title = relationshipNoteTitle(
            fromNoteTopic,
            relationTypeLabel,
            toNoteTopic
          )
          const content = relationshipNoteMarkdown(
            relationTypeLabel,
            fromNoteTopic,
            toNoteTopic,
            bodySuffix
          )
          return this.injectNotes(
            [{ Title: title, Content: content }],
            externalIdentifier,
            notebookName
          )
        })
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

    getNotebookIdByName(notebookName: string) {
      return cy
        .wrap(NotebookController.myNotebooks(), { log: false })
        .then((response) => {
          const data = unwrapData<NotebooksViewedByUser>(response)
          const notebookRealm = data.notebooks.find(
            (realm) => realm.notebook.name === notebookName
          )
          expect(
            notebookRealm,
            `notebook "${notebookName}" was not found for the current user`
          ).to.not.be.undefined
          return notebookRealm!.notebook.id
        })
    },

    getFolderIdInNotebook(notebookId: number, folderName: string) {
      return cy
        .wrap(
          NotebookController.listNotebookFolderIndex({
            path: { notebook: notebookId },
          }),
          { log: false }
        )
        .then((response) => {
          const folders = unwrapData<Folder[]>(response)
          const folder = folders.find((f) => f.name === folderName)
          expect(
            folder,
            `folder "${folderName}" was not found in notebook id ${notebookId}`
          ).to.exist
          return folder!.id
        })
    },

    updateNotebookIndex(notebookName: string) {
      return this.getNotebookIdByName(notebookName).then((notebookId) =>
        cy.wrap(
          NotebookController.updateNotebookIndex({
            path: { notebook: notebookId },
          }),
          { log: false }
        )
      )
    },

    createEmptyFolder(
      notebookName: string,
      folderName: string,
      underNoteTitle?: string
    ) {
      return this.getNotebookIdByName(notebookName).then((notebookId) => {
        const body: FolderCreationRequest = { name: folderName }
        const createFolder = () =>
          cy.wrap(
            NotebookController.createFolder({
              path: { notebook: notebookId },
              body,
            }),
            { log: false }
          )
        if (underNoteTitle) {
          return this.getInjectedNoteIdByTitle(underNoteTitle).then(
            (noteId) => {
              body.underNoteId = noteId
              return createFolder()
            }
          )
        }
        return createFolder()
      })
    },

    createReadmeOnlyFolder(
      notebookName: string,
      folderName: string,
      readme: string
    ) {
      return this.createEmptyFolder(notebookName, folderName).then(
        (response) => {
          const folder = unwrapData<Folder>(response)
          return this.getNotebookIdByName(notebookName).then((notebookId) =>
            cy.wrap(
              NotebookController.updateFolderReadmeContent({
                path: { notebook: notebookId, folder: folder.id },
                body: { content: readme },
              }),
              { log: false }
            )
          )
        }
      )
    },

    setNotebookReadmeContent(notebookName: string, content: string) {
      return this.getNotebookIdByName(notebookName).then((notebookId) =>
        cy.wrap(
          NotebookController.updateNotebookReadmeContent({
            path: { notebook: notebookId },
            body: { content },
          }),
          { log: false }
        )
      )
    },

    setInjectedNoteContent(noteTitle: string, content: string) {
      return this.getInjectedNoteIdByTitle(noteTitle).then((noteId) =>
        cy.wrap(
          TextContentController.updateNoteContent({
            path: { note: noteId },
            body: { content },
          }),
          { log: false }
        )
      )
    },

    /** The note's current content in Donut itself, not an exported markdown file. */
    getInjectedNoteContent(noteTitle: string) {
      return this.getInjectedNoteIdByTitle(noteTitle).then((noteId) =>
        cy
          .wrap(NoteController.showNote({ path: { note: noteId } }), {
            log: false,
          })
          .then((response) => unwrapData<NoteRealm>(response).note.content)
      )
    },

    renameInjectedNoteTitleForNoteOnPage(newTitle: string) {
      return cy.url().then((url) => {
        const noteId = noteIdFromUrl(url)
        return cy.get(`@${injectedNoteIdMapAliasName}`).then((existingMap) => {
          const map = existingMap as Record<string, number>
          const oldKey = Object.keys(map).find((key) => map[key] === noteId)
          if (!oldKey || oldKey === newTitle) {
            return
          }
          const { [oldKey]: _removed, ...rest } = map
          return cy
            .wrap({ ...rest, [newTitle]: noteId })
            .as(injectedNoteIdMapAliasName)
        })
      })
    },

    ...assimilateTestabilityMethods,
    ...bazaarTestabilityMethods,
    ...notebookGitTestabilityMethods,
    ...recallTestabilityMethods,
    ...timeTravelTestabilityMethods,

    startConversationAboutNote(noteTitle: string, message: string) {
      return this.getInjectedNoteIdByTitle(noteTitle).then((noteId) =>
        cy
          .wrap(
            ConversationMessageController.startConversationAboutNote({
              path: { note: noteId },
              body: message,
            }),
            { log: false }
          )
          .then((conversation) => {
            cy.wrap(conversation.id).as(conversationIdAlias(noteTitle))
          })
      )
    },

    replyToConversationAboutNote(
      noteTitle: string,
      messages: readonly string[]
    ) {
      return cy
        .get<number>(`@${conversationIdAlias(noteTitle)}`)
        .then((conversationId) => {
          cy.wrap(messages).each((message: string) => {
            cy.wrap(
              ConversationMessageController.replyToConversation({
                path: { conversationId },
                body: message,
              }),
              { log: false }
            )
          })
        })
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
