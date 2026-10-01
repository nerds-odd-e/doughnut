import {
  findNoteContentRegion,
  richNotePropertyRow,
} from './notePageContentRegion'

function expectPropertyControlHeights(
  $container: JQuery<HTMLElement>,
  testIds: string[],
  context: string,
  tall: boolean
) {
  testIds.forEach((testId) => {
    const control = $container.find(`[data-testid="${testId}"]`)[0]
    expect(control, `control ${testId} of ${context}`).to.exist
    const height = control!.getBoundingClientRect().height
    if (tall) {
      expect(height, `${testId} height`).to.be.at.least(44)
    } else {
      expect(height, `${testId} height`).to.be.below(44)
    }
  })
}

export const notePropertyLayoutMethods = () => ({
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
        expectPropertyControlHeights(
          $row,
          [
            'rich-note-property-panel-toggle',
            'rich-note-property-value-dialog-open',
            'rich-note-property-row-remove',
            'rich-note-property-external-link',
            'rich-note-property-row-key-input',
            'rich-note-property-row-value-input',
          ],
          `"${key}"`,
          tall
        )
      })
    })
    return this
  },
  expectNewRichNotePropertyControlHeights() {
    findNoteContentRegion().should(($region) => {
      expectPropertyControlHeights(
        $region,
        ['rich-note-property-row-add', 'rich-note-property-row-cancel'],
        'the new property',
        true
      )
    })
    return this
  },
  expectNewRichNotePropertyControlNotCovered(testId: string) {
    cy.get('[data-property-draft="true"]')
      .findByTestId(testId)
      .should(($control) => {
        const box = $control[0]!.getBoundingClientRect()
        const list = $control[0]!.ownerDocument.querySelector(
          '[data-testid="rich-note-property-key-preset-list"]'
        )
        const covering = list
          ? [list, ...list.querySelectorAll('button')].filter((element) => {
              const rect = element.getBoundingClientRect()
              return (
                rect.left < box.right &&
                box.left < rect.right &&
                rect.top < box.bottom &&
                box.top < rect.bottom
              )
            })
          : []
        expect(covering.length, `preset list over ${testId}`).to.equal(0)
      })
    return this
  },
  expectKeyPresetInsideKeyPanel(presetKey: string) {
    cy.findByTestId('rich-note-property-key-preset-list').should(($list) => {
      const panelRight = $list[0]!.parentElement!.getBoundingClientRect().right
      const option = $list.find(`[data-preset-key="${presetKey}"]`)[0]!
      expect(
        option.getBoundingClientRect().right,
        'option right edge'
      ).to.be.at.most(panelRight)
      expect(option.scrollWidth, 'option scroll width').to.be.at.most(
        option.clientWidth
      )
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
})
