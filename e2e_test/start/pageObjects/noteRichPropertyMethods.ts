import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  confirmPropertyMemoryTrackerChange,
  expectRichNotePropertyRowFocused,
  findNoteContentRegion,
  richNotePropertyRow,
} from './notePageContentRegion'
import { assumeAssociateWikidataDialog } from './associateWikidataDialog'

export const noteRichPropertyMethods = () => ({
  addRichNoteProperty(key: string, value: string) {
    findNoteContentRegion().within(() => {
      cy.findByRole('button', { name: 'Add property' }).click()
      cy.findByTestId('rich-note-property-key')
        .clear()
        .type(key, { parseSpecialCharSequences: false })
      cy.findByTestId('rich-note-property-value')
        .clear()
        .type(value, { parseSpecialCharSequences: false })
        .blur()
    })
    findNoteContentRegion().within(() => {
      cy.get('.ql-editor[contenteditable="true"]').first().click()
    })
    return this.flushPendingContentSave()
  },
  uploadRichNoteImagePropertyFromFixture(fixtureRelativePath: string) {
    findNoteContentRegion().within(() => {
      cy.findByRole('button', { name: 'Add property' }).click()
      cy.findByTestId('rich-note-property-key').clear().type('image')
      cy.get('[data-testid="rich-note-image-insert-file-input"]').selectFile(
        `e2e_test/fixtures/${fixtureRelativePath}`,
        { force: true }
      )
    })
    cy.get(richNotePropertyRow('image'), { timeout: 20000 }).should('exist')
    return this.flushPendingContentSave()
  },
  setRichNoteImagePropertyUrl(url: string) {
    this.addRichNoteProperty('image', url)
    cy.get(richNotePropertyRow('image'), { timeout: 20000 }).should('exist')
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
  expectRichNotePropertyDisplayed(key: string, value: string) {
    findNoteContentRegion().within(() => {
      cy.contains('h4', 'Properties')
      cy.get(richNotePropertyRow(key)).within(() => {
        cy.get('[data-testid="rich-note-property-row-key-input"]').should(
          'have.value',
          key
        )
        const keyNorm = key.trim().toLowerCase()
        const isWikidata = keyNorm === 'wikidata_id' || keyNorm === 'wikidataid'
        if (isWikidata) {
          cy.contains('.font-mono', value).should('exist')
        } else if (keyNorm === 'image') {
          cy.get('[data-testid="rich-note-property-row-value-input"]').should(
            'have.value',
            value.trim()
          )
        } else {
          cy.get('[data-testid="rich-note-property-row-value-input"]').should(
            ($el) => {
              expect($el.text().trim()).to.eq(value)
            }
          )
        }
      })
    })
    return this
  },
  expectRichNoteImagePropertyAttachmentPath(key: string) {
    findNoteContentRegion().within(() => {
      cy.get(richNotePropertyRow(key)).within(() => {
        cy.get('[data-testid="rich-note-property-row-value-input"]').should(
          ($input) => {
            const actual = String($input.val() ?? '').trim()
            expect(
              actual,
              `Expected rich note property "${key}" to be an attachment image path (/attachments/images/...), but found ${JSON.stringify(actual)}`
            ).to.match(/^\/attachments\/images\/\d+\/.+/)
          }
        )
      })
    })
    return this
  },
  expectRichNotePropertyAbsent(key: string) {
    findNoteContentRegion().within(() => {
      cy.get(richNotePropertyRow(key)).should('not.exist')
    })
    return this
  },
  removeRichNoteProperty(key: string) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      cy.get(richNotePropertyRow(key)).within(() => {
        cy.findByTestId('rich-note-property-row-options-toggle').click({
          force: true,
        })
        cy.findByTestId('rich-note-property-row-remove').click({ force: true })
      })
    })
    confirmPropertyMemoryTrackerChange()
    return this.flushPendingContentSave()
  },
  renameFocusedRichNotePropertyKey(oldKey: string, newKey: string) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      cy.get(richNotePropertyRow(oldKey), { timeout: 15000 }).within(() => {
        cy.get('[data-testid="rich-note-property-row-key-input"]')
          .focus({ force: true })
          .clear({ force: true })
          .type(newKey, { force: true, parseSpecialCharSequences: false })
          .blur({ force: true })
      })
    })
    cy.get('.dirty').should('not.exist')
    waitUntilAppIsNotBusy()
    return this
  },
  editRichNoteProperty(oldKey: string, newKey: string, newValue: string) {
    // Edit value before key: changing the key updates `data-property-key` on the row,
    // which breaks a single `.within()` chain that queries by `oldKey` then touches both inputs.
    findNoteContentRegion().within(() => {
      cy.contains('h4', 'Properties')
      cy.get(richNotePropertyRow(oldKey), { timeout: 15000 }).within(() => {
        cy.get('[data-testid="rich-note-property-row-value-input"]')
          .clear()
          .type(newValue)
          .blur()
      })
    })
    findNoteContentRegion().within(() => {
      cy.get(richNotePropertyRow(oldKey), { timeout: 15000 }).within(() => {
        cy.get('[data-testid="rich-note-property-row-key-input"]')
          .clear()
          .type(newKey)
          .blur()
      })
    })
    findNoteContentRegion().within(() => {
      cy.get('.ql-editor[contenteditable="true"]').first().click()
    })
    return this.flushPendingContentSave()
  },
  expectWikidataBrowseLinkOpensUrl(expectedUrl: string) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      cy.get('[data-testid="rich-note-property-row"]')
        .filter((_, row) => {
          const key = row.getAttribute('data-property-key')?.toLowerCase()
          return key === 'wikidata_id' || key === 'wikidataid'
        })
        .find('[data-testid="rich-note-property-external-link"]')
        .should('be.visible')
        .then(($btn) => {
          cy.window().then((win) => {
            const popupWindowStub = {
              location: { href: undefined as string | undefined },
              focus: cy.stub(),
            }
            cy.stub(win, 'open').as('open').returns(popupWindowStub)
            cy.wrap($btn).click()
            cy.get('@open').should('have.been.called')
            cy.wrap(() => popupWindowStub.location.href)
              .should((cb) => {
                const actualUrl = cb()
                expect(
                  actualUrl,
                  `Expected Wikidata association to open ${expectedUrl}, but opened ${actualUrl}`
                ).to.equal(expectedUrl)
              })
              .then(() => {
                expect(popupWindowStub.focus).to.have.been.called
              })
          })
        })
    })
    return this
  },
  associateWikidataDialog() {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      cy.root().then(($region) => {
        const editBtn = $region.find(
          '[data-testid="rich-note-wikidata-property-edit"]'
        )
        if (editBtn.length > 0) {
          cy.wrap(editBtn.first()).click()
        } else {
          cy.findByRole('button', { name: 'Add property' }).click()
          cy.findByTestId('rich-note-property-key').clear().type('wikidata_id')
          cy.findByTestId('rich-note-wikidata-property-insert-edit').click()
        }
      })
    })
    return assumeAssociateWikidataDialog()
  },
})
