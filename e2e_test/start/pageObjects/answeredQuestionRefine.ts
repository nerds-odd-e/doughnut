import {
  openRefineNoteModalIfNeeded,
  refinementLayoutPanel,
} from './assimilationPage/shared'

function layoutCheckboxForPoint(layoutPointText: string) {
  return refinementLayoutPanel()
    .contains('[data-layout-level] > label', layoutPointText)
    .find('input[type="checkbox"]')
    .first()
}

export function answeredQuestionRefineMethods<T extends object>(self: T) {
  return {
    openRefineNoteModal() {
      openRefineNoteModalIfNeeded()
      refinementLayoutPanel().scrollIntoView().should('be.visible')
      return self
    },
    expectRefinementLayoutPointsSelected(...layoutPointTexts: string[]) {
      this.openRefineNoteModal()
      layoutPointTexts.forEach((layoutPointText) => {
        layoutCheckboxForPoint(layoutPointText).should('be.checked')
      })
      return self
    },
    expectRefinementLayoutPointsNotSelected(...layoutPointTexts: string[]) {
      this.openRefineNoteModal()
      layoutPointTexts.forEach((layoutPointText) => {
        layoutCheckboxForPoint(layoutPointText).should('not.be.checked')
      })
      return self
    },
  }
}
