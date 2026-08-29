import type { RouteLocationNamedRaw } from 'vue-router'
import { namedLocationHref } from '@/routes/namedLocationHref'
import {
  notePropertyLocation,
  noteShowLocation,
} from '@/routes/noteShowLocation'
import testability from '../testability'
import {
  findNoteContentRegion,
  richNotePropertyRow,
} from './notePageContentRegion'

function expectCompiledLocation(
  location: RouteLocationNamedRaw,
  query?: Record<string, string>
) {
  const expected = namedLocationHref({ ...location, query })
  cy.location().should((loc) => {
    const actual = `${loc.pathname}${loc.search}`
    expect(
      actual,
      `Expected location ${JSON.stringify(expected)}, but found ${JSON.stringify(actual)}`
    ).to.equal(expected)
  })
}

function expectAtCompiledNoteLocation(
  noteTopology: string,
  locationForNoteId: (noteId: number) => RouteLocationNamedRaw,
  query?: Record<string, string>
) {
  testability()
    .getInjectedNoteIdByTitle(noteTopology)
    .then((noteId: number) => {
      expectCompiledLocation(locationForNoteId(noteId), query)
    })
}

export const notePropertyLocationMethods = () => ({
  openRichNotePropertyValuePanel(key: string) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      cy.get(richNotePropertyRow(key))
        .find('[data-testid="rich-note-property-value-popup-open"]')
        .click()
    })
    return this
  },
  closePropertyValuePanel() {
    cy.get('dialog')
      .filter(':visible')
      .find('[data-testid="rich-note-property-value-popup-cancel"]')
      .click()
    return this
  },
  expectAtNoteProperty(
    noteTopology: string,
    propertyKey: string,
    query?: Record<string, string>
  ) {
    expectAtCompiledNoteLocation(
      noteTopology,
      (noteId) => notePropertyLocation(noteId, propertyKey),
      query
    )
    return this
  },
  expectAtNoteShow(noteTopology: string, query?: Record<string, string>) {
    expectAtCompiledNoteLocation(
      noteTopology,
      (noteId) => noteShowLocation(noteId),
      query
    )
    return this
  },
  expectPropertyValueDialogClosed() {
    cy.get('dialog').should('not.exist')
    return this
  },
})
