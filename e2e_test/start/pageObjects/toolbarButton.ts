import { submittableForm } from '../forms'

const escapeRegex = (value: string) =>
  value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/** Match toolbar labels that may include keyboard shortcuts, e.g. "Edit as markdown (m)". */
const toolbarButtonName = (btnTextOrTitle: string) =>
  new RegExp(`^${escapeRegex(btnTextOrTitle)}(\\s|$|\\()`)

export const toolbarButton = (btnTextOrTitle: string) => {
  const getButton = () =>
    cy.findByRole('button', { name: toolbarButtonName(btnTextOrTitle) })
  return {
    click: () => {
      getButton().click()
      return { ...submittableForm }
    },
    shouldNotExist: () => getButton().should('not.exist'),
  }
}
