import submittableForm from '../forms'

export const toolbarButton = (btnTextOrTitle: string) => {
  const getButton = () => cy.findByRole('button', { name: btnTextOrTitle })
  return {
    click: () => {
      getButton().click()
      return { ...submittableForm }
    },
    shouldNotExist: () => getButton().should('not.exist'),
  }
}
