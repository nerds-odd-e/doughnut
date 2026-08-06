import {
  layoutCheckboxForPoint,
  openRefineNoteModalIfNeeded,
  refinementLayoutPanel,
} from './assimilationPage/shared'

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
