import { nonInteractiveOutput } from './outputAssertions'
import testability from '../../testability'
import { notebookCloneCheckout } from './notebookCloneCheckout'
import type { PublicationProfileParameters } from '../../../config/notebookPublicationProfile'

const notebook = 'CLI Clone Notebook'
const title = (kind: string, i: number) =>
  `${kind}-${String(i).padStart(5, '0')}`
const folder = (i: number) => `group-${String(i).padStart(2, '0')}`
function document(kind: string, i: number, existing: number, count: number) {
  return `---\ntype: Note\naliases: ['${kind} alias ${i}']\nmeaning: '${kind} concept ${i}'\nsource: '[[${title('Existing', i % existing)}]]'\nrelated:\n  - '[[${title('Existing', (i + 1) % existing)}]]'\n  - '[[${title('Existing', (i + 2) % existing)}]]'\n---\n${kind} concept ${i}. ${'Deterministic authored prose. '.repeat(32)}\nSee [[${title('Existing', i % existing)}]] and [[${title(kind, (i + 1) % count)}]].\n`
}

function recordPublicationTiming(started: string) {
  return cy.task('recordNotebookPublicationTiming', {
    started,
    stopped: new Date().toISOString(),
    boundary:
      'installed CLI execution, includes bundle preparation and client work',
  })
}

function changedFiles(
  pathKind: string,
  contentKind: string,
  count: number,
  existing: number,
  folders: number
) {
  return Array.from({ length: count }, (_, i) => ({
    relativePath: `${folder(i % folders)}/${title(pathKind, i)}.md`,
    content: document(contentKind, i, existing, count),
  }))
}

function stageProposal(files: { relativePath: string; content: string }[]) {
  notebookCloneCheckout().commitRelatedNoteChanges(files)
  return cy
    .get<string>('@cliCloneDestination')
    .then((checkoutDir) =>
      cy
        .task<string>('normalizeNotebookPublicationProposal', checkoutDir)
        .as('cliNotebookPublishHead')
    )
}

function publishHttp() {
  return cy
    .get<string>('@cliCloneDestination')
    .then((checkoutDir) =>
      cy
        .get<string>('@cliConfigDir')
        .then((configDir) =>
          cy.task<{ status: number; body: string }>(
            'publishNotebookPublicationHttp',
            { checkoutDir, configDir }
          )
        )
    )
}

export const notebookPublicationProfile = {
  publishHttp() {
    return publishHttp().then((response) => {
      expect(response.status, response.body).to.equal(200)
      return cy
        .get<string>('@cliNotebookPublishHead')
        .then((head) =>
          expect(response.body.trim(), 'HTTP accepted head').to.equal(head)
        )
    })
  },
  publishHttpRejection() {
    return publishHttp().then((response) => {
      expect(response.status, response.body).to.equal(400)
      return cy
        .get<string>('@publicationProfileInvalidPath')
        .then((path) =>
          expect(JSON.parse(response.body).message).to.contain(
            `Invalid authored property at path "${path}"`
          )
        )
    })
  },
  seed() {
    return cy
      .task<PublicationProfileParameters>(
        'notebookPublicationProfileParameters'
      )
      .then((parameters) => {
        cy.wrap(parameters).as('publicationProfileParameters')
        testability().backendTimeTravelTo(0, 12)
        cy.wrap(Array.from({ length: parameters.folders }, (_, i) => i)).each(
          (i) =>
            testability().createReadmeOnlyFolder(
              notebook,
              folder(i),
              'Profile folder landing'
            )
        )
        cy.get<string>('@injectNotesExternalIdentifier').then(
          (externalIdentifier) =>
            testability().injectNotes(
              Array.from({ length: parameters.existing }, (_, i) => ({
                Title: title('Existing', i),
                Folder: folder(i % parameters.folders),
                Content: document(
                  'Existing',
                  i,
                  parameters.existing,
                  parameters.existing
                ),
              })),
              externalIdentifier,
              notebook
            )
        )
        testability().assimilateNote(title('Existing', 0))
        testability()
          .memoryTrackerForNote(title('Existing', 0), 'UNDERSTANDING')
          .as('publicationProfileLearning')
        return cy.then(() =>
          testability().resnapshotNotebookGitBindingForTestability(notebook)
        )
      })
  },
  prepare() {
    return cy
      .get<PublicationProfileParameters>('@publicationProfileParameters')
      .then((parameters) =>
        stageProposal(
          changedFiles(
            'Added',
            'Added',
            parameters.additions,
            parameters.existing,
            parameters.folders
          )
        )
      )
  },
  prepareEdit() {
    return cy
      .get<PublicationProfileParameters>('@publicationProfileParameters')
      .then((parameters) =>
        stageProposal(
          changedFiles(
            'Existing',
            'Updated',
            parameters.updates,
            parameters.existing,
            parameters.folders
          )
        )
      )
  },
  invalidateLast() {
    return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
      cy
        .task<string>('invalidateLastNotebookPublicationDocument', checkoutDir)
        .then((path) => {
          cy.wrap(path).as('publicationProfileInvalidPath')
          return cy
            .task<string>('normalizeNotebookPublicationProposal', checkoutDir)
            .as('cliNotebookPublishHead')
        })
    )
  },
  publishRejection() {
    return cy.then(() => {
      const started = new Date().toISOString()
      return notebookCloneCheckout()
        .publishExpectingRejection()
        .then(() => {
          recordPublicationTiming(started)
          return cy
            .get<string>('@publicationProfileInvalidPath')
            .then((path) =>
              nonInteractiveOutput().expectContains(
                `Invalid authored property at path "${path}"`
              )
            )
        })
    })
  },
  expectPreserved() {
    return cy.get<string>('@cliCloneReceiverDestination').then((checkoutDir) =>
      cy.get<string>('@publicationProfileInvalidPath').then((invalidPath) =>
        cy.task('confirmRejectedNotebookPublicationProfile', {
          checkoutDir,
          invalidPath,
        })
      )
    )
  },
  publish() {
    return cy.then(() => {
      const started = new Date().toISOString()
      return notebookCloneCheckout()
        .publish()
        .then(() => recordPublicationTiming(started))
    })
  },
  start() {
    return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
      cy.get('@publicationProfileLearning').then((learning) =>
        cy.task('startNotebookPublicationProfile', {
          checkoutDir,
          learning,
        })
      )
    )
  },
  stop() {
    return cy.task('stopNotebookPublicationProfile')
  },
  expectReceived() {
    return cy
      .get<{ relativePath: string; content: string }[]>(
        '@cliNotebookProposalFiles'
      )
      .then((files) =>
        cy
          .get<string>('@cliCloneReceiverDestination')
          .then((checkoutDir) =>
            cy.task('verifyPublicationReceiverFiles', { checkoutDir, files })
          )
          .then(() =>
            cy.get<string>('@cliNotebookPublishHead').then((acceptedHead) =>
              cy.task('confirmNotebookPublicationProfile', {
                acceptedHead,
                documentCount: files.length,
              })
            )
          )
      )
  },
}
