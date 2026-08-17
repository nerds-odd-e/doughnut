import {
  layoutCheckboxForItem,
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
    expectRefinementLayoutItemsSelected(...layoutItemTexts: string[]) {
      this.openRefineNoteModal()
      layoutItemTexts.forEach((layoutItemText) => {
        layoutCheckboxForItem(layoutItemText).should('be.checked')
      })
      return self
    },
    expectRefinementLayoutItemsNotSelected(...layoutItemTexts: string[]) {
      this.openRefineNoteModal()
      layoutItemTexts.forEach((layoutItemText) => {
        layoutCheckboxForItem(layoutItemText).should('not.be.checked')
      })
      return self
    },
  }
}
