/**
 * TEMPORARY MEASUREMENT MACHINERY — SEED-034#story-4, slices 2-3 and 13.
 * Deleted together with `note_save_measurement.feature` in slice 14, which
 * documents how to run it and what it is for.
 *
 * Each ordinary content save in a large synchronized notebook reports three
 * separate boundaries:
 *
 *   keystrokeToSettledMs — last keystroke until the editor shows the saved
 *                          content again (contains the real one-second
 *                          debounce; never attribute that to storage)
 *   requestMs            — the content-save request/response alone
 *   keystrokeToRequestMs — what the debounce, or the product's immediate
 *                          new-wiki-link flush, costs before storage is asked
 *                          to do any work
 *
 * It uses the product's own editor, debounce and serialized persist chain.
 * No production timing hook, no mock clock, no benchmark framework.
 */
import type { NoteSaveFixture } from '../../config/noteSaveMeasurement'
import { navigationActions } from '../actions/navigationActions'
import { waitUntilAppIsNotBusy } from '../pageBase'
import testability from '../testability'
import { noteContentEditingMethods } from './noteContentEditingMethods'
import { runNodeSideNotebookTask } from './noteSaveMeasurementNotebookTask'
import { findNoteContentRegion } from './notePageContentRegion'

/** Seeding, saving and exporting a whole notebook all need far more than the
 * 6s default. */
const MEASUREMENT_TIMEOUT_MS = 600_000

const CONTENT_SAVE_ROUTE = '**/api/text_content/*/content'

type RequestTiming = { startedAt: number; endedAt: number }

type Sample = {
  edit: string
  sample: number
  note: string
  typed: string
  linkTarget?: string
  keystrokeToSettledMs: number
  keystrokeToRequestMs: number
  requestMs: number
}

/** What one measured edit types, and the wiki link it adds, if any. */
type MeasuredEdit = Pick<Sample, 'edit' | 'typed' | 'linkTarget'>

const fixtureAlias = 'noteSaveFixture'
const timingsAlias = 'noteSaveRequestTimings'
const samplesAlias = 'noteSaveSamples'

/** The note every sample edits: the notebook's first note, which already
 * carries resolving wiki links from the generated fixture. */
const measuredNote = (fixture: NoteSaveFixture) => fixture.notes[0]!.Title

/** A note the measured note does not already reference, distinct per sample,
 * so every added-link edit really adds a new resolving wiki link. The measured
 * note's own generated references are notes 1, 2, 3 and `folders`, far below
 * the middle of the notebook. */
const addedLinkTarget = (fixture: NoteSaveFixture, sampleNumber: number) =>
  fixture.notes[Math.floor(fixture.notes.length / 2) + sampleNumber * 7]!.Title

function markdownEditor() {
  noteContentEditingMethods().openMarkdownContentEditor()
  return findNoteContentRegion().find('textarea').filter(':visible').first()
}

export const noteSaveMeasurement = () => ({
  seedLargeSynchronizedNotebook() {
    cy.task<NoteSaveFixture>('noteSaveMeasurementFixture', null, {
      timeout: MEASUREMENT_TIMEOUT_MS,
    }).then((fixture) => {
      cy.wrap(fixture, { log: false }).as(fixtureAlias)
      cy.wrap([] as Sample[], { log: false }).as(samplesAlias)
      // Shared seeding helpers wrap their request in a default-timeout
      // command; a whole notebook needs longer than any ordinary step does.
      const ordinaryCommandTimeout = Cypress.config('defaultCommandTimeout')
      Cypress.config('defaultCommandTimeout', MEASUREMENT_TIMEOUT_MS)
      const seeding = { startedAt: Date.now(), injectedAt: 0 }
      cy.get<string>('@currentLoginUser').then((username) =>
        testability().injectNotes(fixture.notes, username, fixture.notebook)
      )
      cy.then(() => {
        seeding.injectedAt = Date.now()
        return testability().resnapshotNotebookGitBindingForTestability(
          fixture.notebook
        )
      })
      cy.then(() =>
        Cypress.config('defaultCommandTimeout', ordinaryCommandTimeout)
      )
      cy.then(() =>
        cy.task('reportNoteSaveMeasurement', {
          seeded: fixture.notebook,
          notes: fixture.notes.length,
          injectNotesMs: seeding.injectedAt - seeding.startedAt,
          acceptedBaselineSnapshotMs: Date.now() - seeding.injectedAt,
          totalSeedingMs: Date.now() - seeding.startedAt,
        })
      )
      const timings: RequestTiming[] = []
      cy.intercept({ method: 'PATCH', url: CONTENT_SAVE_ROUTE }, (req) => {
        const startedAt = Date.now()
        req.on('response', () =>
          timings.push({ startedAt, endedAt: Date.now() })
        )
      }).as('contentSave')
      cy.wrap(timings, { log: false }).as(timingsAlias)
    })
    return this
  },

  /** TEMPORARY (slice 13): the fixture's root attachments, if it has any,
   * published through Git before any measured save. */
  publishFixtureRootAttachments() {
    return this.runNotebookTask('publishNoteSaveMeasurementAttachments')
  },

  runNotebookTask(task: string) {
    cy.get<NoteSaveFixture>(`@${fixtureAlias}`).then((fixture) =>
      runNodeSideNotebookTask(fixture.notebook, task, MEASUREMENT_TIMEOUT_MS)
    )
    return this
  },

  /** One save of each kind whose timing is reported but discarded, so the
   * sampled saves are not measuring a cold application. */
  warmUpTheMeasuredSavePath() {
    this.measureSaveKeepingExistingWikiLinks(0)
    this.measureSaveAddingAWikiLink(0)
    return this
  },

  /** Evidence that the two compared sides hold the same notebook content. */
  capturePortableExportForComparison() {
    return this.runNotebookTask('saveNoteSaveMeasurementExport')
  },

  measureSaveKeepingExistingWikiLinks(sampleNumber: number) {
    return this.measureSave(sampleNumber, () => ({
      edit: 'existing wiki links',
      typed: ` Measured observation ${sampleNumber}.`,
    }))
  },

  measureSaveAddingAWikiLink(sampleNumber: number) {
    return this.measureSave(sampleNumber, (fixture) => {
      const linkTarget = addedLinkTarget(fixture, sampleNumber)
      const typed = ` See [[${linkTarget}]]`
      return { edit: 'added wiki link', typed, linkTarget }
    })
  },

  /** One changed save, start to settled, before any next sample begins. */
  measureSave(
    sampleNumber: number,
    describeEdit: (fixture: NoteSaveFixture) => MeasuredEdit
  ) {
    cy.get<NoteSaveFixture>(`@${fixtureAlias}`).then((fixture) => {
      const { edit, typed, linkTarget } = describeEdit(fixture)
      navigationActions.jumpToNotePage(measuredNote(fixture))
      waitUntilAppIsNotBusy()
      const lastKeystroke = { at: 0 }
      cy.get<RequestTiming[]>(`@${timingsAlias}`).then((timings) => {
        timings.length = 0
      })
      markdownEditor()
        // The real last keystroke, not the moment Cypress finishes typing:
        // an added wiki link flushes immediately, so its request can start
        // before `.type()` returns.
        .then(($editor) =>
          $editor.on('input', () => {
            lastKeystroke.at = Date.now()
          })
        )
        .type(typed, { delay: 12 })
      cy.wait('@contentSave', { timeout: MEASUREMENT_TIMEOUT_MS })
      cy.get('.dirty', { timeout: MEASUREMENT_TIMEOUT_MS }).should('not.exist')
      waitUntilAppIsNotBusy()
      cy.get<RequestTiming[]>(`@${timingsAlias}`).then((timings) => {
        const settledAt = Date.now()
        expect(
          timings.length,
          `${edit}: exactly one content-save request per measured save`
        ).to.equal(1)
        const sample: Sample = {
          edit: sampleNumber === 0 ? `warm-up ${edit}` : edit,
          sample: sampleNumber,
          note: measuredNote(fixture),
          typed,
          linkTarget,
          keystrokeToSettledMs: settledAt - lastKeystroke.at,
          keystrokeToRequestMs: timings[0]!.startedAt - lastKeystroke.at,
          requestMs: timings[0]!.endedAt - timings[0]!.startedAt,
        }
        cy.get<Sample[]>(`@${samplesAlias}`).then((samples) =>
          samples.push(sample)
        )
        cy.task('reportNoteSaveMeasurement', sample)
      })
    })
    return this
  },

  expectBothEditKindsMeasuredWithSeparateBoundaries() {
    cy.get<Sample[]>(`@${samplesAlias}`).should((samples) => {
      expect(
        samples.map((sample) => sample.edit),
        'both measured edit kinds'
      ).to.include.members(['existing wiki links', 'added wiki link'])
      for (const sample of samples) {
        expect(
          sample.keystrokeToSettledMs,
          `${sample.edit}: settled editor must be later than its own request alone`
        ).to.be.greaterThan(sample.requestMs)
      }
    })
    return this
  },

  expectMeasuredNoteReloadsWithEveryMeasuredEdit() {
    cy.get<NoteSaveFixture>(`@${fixtureAlias}`).then((fixture) => {
      navigationActions.jumpToNotePage(measuredNote(fixture))
      cy.reload()
      waitUntilAppIsNotBusy()
      cy.get<Sample[]>(`@${samplesAlias}`).then((samples) => {
        for (const sample of samples) {
          if (sample.linkTarget)
            findNoteContentRegion()
              .find('a.donut-wiki-link')
              .contains(sample.linkTarget)
              .should('exist')
        }
        const editing = noteContentEditingMethods().openMarkdownContentEditor()
        for (const sample of samples) {
          editing.expectMarkdownContentSourceContains(sample.typed.trim())
        }
      })
    })
    return this
  },
})
