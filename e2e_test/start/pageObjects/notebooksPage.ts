import { assumeNotePage } from './notePage';
import NotePath from "../../support/NotePath"

export const routerToNotebooksPage = () => {
  cy.routerPush("/notebooks", "notebooks", {})
  return {
    navigateToPath(notePath: NotePath) {
      notePath.path.forEach((noteTopic) => assumeNotePage().navigateToChild(noteTopic))
    },
  }
}
