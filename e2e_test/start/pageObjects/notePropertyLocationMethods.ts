import type { RouteLocationNamedRaw } from 'vue-router'
import { namedLocationHref } from '@/routes/namedLocationHref'
import {
  notePropertyLocation,
  noteShowLocation,
} from '@/routes/noteShowLocation'
import testability from '../testability'
import {
  expectRichNotePropertyRowFocused,
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
  openRichNotePropertyPanel(key: string) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      cy.get(richNotePropertyRow(key))
        .find('[data-testid="rich-note-property-row-options-toggle"]')
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
  closeRichNotePropertyPanel() {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      cy.get(
        '[data-testid="rich-note-property-row-options-toggle"][aria-expanded="true"]'
      ).click({ force: true })
    })
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
  expectFocusedRichNoteProperty(key: string) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      expectRichNotePropertyRowFocused(key)
    })
    cy.get('dialog')
      .filter(':visible')
      .should('be.visible')
      .within(() => {
        cy.contains('h2', key).should('be.visible')
        cy.get(
          '[data-testid="rich-note-property-value-popup-textarea"]'
        ).should('be.visible')
      })
    return this
  },
  expectFocusedRichNotePropertyPanel(key: string) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      expectRichNotePropertyRowFocused(key)
      cy.get(richNotePropertyRow(key)).within(() => {
        cy.get('[data-testid="rich-note-property-row-options"]').should(
          'be.visible'
        )
        cy.get('[data-testid="rich-note-property-row-options-toggle"]').should(
          'have.attr',
          'aria-expanded',
          'true'
        )
      })
    })
    return this
  },
  expectFocusedRichNotePropertyValueWithoutDialog(key: string, value: string) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      expectRichNotePropertyRowFocused(key).and(($row) => {
        const actual = $row.text()
        expect(
          actual,
          `Expected focused property "${key}" to show ${JSON.stringify(value)}, but found ${JSON.stringify(actual.trim())}`
        ).to.include(value)
      })
    })
    cy.get('dialog').should('not.exist')
    return this
  },
  expectRichNotePropertyNotFound(key: string) {
    this.switchToRichContent()
    const expected = `Property "${key}" not found`
    findNoteContentRegion().within(() => {
      cy.get('[data-testid="rich-note-property-not-found"]').should(($el) => {
        const actual = $el.text().trim()
        expect(
          actual,
          `Expected property-not-found state ${JSON.stringify(expected)}, but found ${JSON.stringify(actual)}`
        ).to.equal(expected)
      })
      cy.get('[data-property-focused="true"]').should('not.exist')
    })
    cy.get('dialog').should('not.exist')
    return this
  },
})
