import { pageIsNotLoading } from '../pageBase'
import {
  confirmPropertyMemoryTrackerChange,
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
            expect(String($input.val() ?? '').trim()).to.match(
              /^\/attachments\/images\/\d+\/.+/
            )
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
      cy.get(richNotePropertyRow(key))
        .find('[data-testid="rich-note-property-row-remove"]')
        .click()
    })
    confirmPropertyMemoryTrackerChange()
    return this.flushPendingContentSave()
  },
  removeMarkdownNotePropertyConfirmingMemoryTrackerChange(key: string) {
    this.openMarkdownContentEditor()
    cy.get('textarea').then(($ta) => {
      const lines = String($ta.val() ?? '').split('\n')
      const withoutProperty = lines
        .filter((line) => !new RegExp(`^\\s*${key}\\s*:`).test(line))
        .join('\n')
      cy.wrap($ta).clear().invoke('val', withoutProperty).trigger('input')
    })
    findNoteContentRegion().find('textarea').filter(':visible').first().blur()
    cy.get('body').click(0, 0, { force: true })
    confirmPropertyMemoryTrackerChange()
    cy.get('.dirty').should('not.exist')
    pageIsNotLoading()
    return this
  },
  renameRichNotePropertyKey(oldKey: string, newKey: string) {
    this.switchToRichContent()
    findNoteContentRegion().within(() => {
      cy.get(richNotePropertyRow(oldKey), { timeout: 15000 }).within(() => {
        cy.get('[data-testid="rich-note-property-row-key-input"]')
          .clear()
          .type(newKey)
          .blur()
      })
    })
    confirmPropertyMemoryTrackerChange()
    return this.flushPendingContentSave()
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
              .should((cb) => expect(cb()).equal(expectedUrl))
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
