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
  richNotePropertyPanel,
  richNotePropertyPanelToggle,
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
      cy.get(richNotePropertyRow(key)).find(richNotePropertyPanelToggle).click()
    })
    return this
  },
  closeRichNotePropertyPanel() {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      cy.get(`${richNotePropertyPanelToggle}[aria-expanded="true"]`).click({
        force: true,
      })
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
  expectFocusedRichNotePropertyPanel(key: string) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      expectRichNotePropertyRowFocused(key)
      cy.get(richNotePropertyRow(key)).within(() => {
        cy.get(richNotePropertyPanel).should('be.visible')
        cy.get(richNotePropertyPanelToggle).should(
          'have.attr',
          'aria-expanded',
          'true'
        )
      })
    })
    return this
  },
  expectFocusedRichNotePropertyValue(key: string, value: string) {
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
    return this
  },
  expectRichNotePropertyValueVisible(key: string) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      cy.get(richNotePropertyRow(key))
        .find('dd')
        .should('be.visible')
        .and(($value) => {
          expect($value.width(), `width of the value of "${key}"`).to.be.above(
            100
          )
        })
    })
    return this
  },
  expectEditablePropertyValueShowsAllText(key: string) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      cy.get(richNotePropertyRow(key))
        .find('[data-testid="rich-note-property-row-value-input"]')
        .should(($value) => {
          const field = $value[0]!
          expect(
            field.scrollHeight,
            `scroll height of the value of "${key}"`
          ).to.be.at.most(field.clientHeight)
        })
    })
    return this
  },
  expectRichNotePropertyKeyEndsWithEllipsis(key: string) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      cy.get(richNotePropertyRow(key))
        .find('[data-testid="rich-note-property-row-key-input"]')
        .should('have.css', 'text-overflow', 'ellipsis')
    })
    return this
  },
  expectRichNotePropertyControlsApart(key: string) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      cy.get(richNotePropertyRow(key)).should(($row) => {
        const boxes = [
          '[data-testid="rich-note-property-panel-toggle"]',
          '[data-testid="rich-note-property-value-dialog-open"]',
          '[data-testid="rich-note-property-row-remove"]',
        ].map((selector) => {
          const control = $row.find(selector)[0]
          expect(control, `control ${selector} of "${key}"`).to.exist
          return control!.getBoundingClientRect()
        })
        boxes.forEach((a, i) =>
          boxes.slice(i + 1).forEach((b) => {
            const apart =
              a.right <= b.left ||
              b.right <= a.left ||
              a.bottom <= b.top ||
              b.bottom <= a.top
            expect(apart, `controls of "${key}" apart`).to.be.true
          })
        )
      })
    })
    return this
  },
  expectRichNotePropertyControlHeights(key: string, tall: boolean) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      cy.get(richNotePropertyRow(key)).should(($row) => {
        ;[
          'rich-note-property-panel-toggle',
          'rich-note-property-value-dialog-open',
          'rich-note-property-row-remove',
          'rich-note-property-external-link',
          'rich-note-property-row-key-input',
          'rich-note-property-row-value-input',
        ].forEach((testId) => {
          const control = $row.find(`[data-testid="${testId}"]`)[0]
          expect(control, `control ${testId} of "${key}"`).to.exist
          const height = control!.getBoundingClientRect().height
          if (tall) {
            expect(height, `${testId} height`).to.be.at.least(44)
          } else {
            expect(height, `${testId} height`).to.be.below(44)
          }
        })
      })
    })
    return this
  },
  expectNoteWithoutSidewaysScroll() {
    cy.document().should((doc) => {
      const page = doc.documentElement
      expect(page.scrollWidth, 'page scroll width').to.be.at.most(
        page.clientWidth
      )
    })
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
    return this
  },
})
