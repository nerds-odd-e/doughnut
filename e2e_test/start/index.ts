import basicActions from './basicActions'
import mock_services from './mock_services/index'
import { questionGenerationService } from './questionGenerationService'
import testability from './testability'
import {
  assimilation,
  assumeAssimilationPage,
} from './pageObjects/assimilationPage'
import { recall } from './pageObjects/recallPage'
import { assumeCirclePage } from './pageObjects/circlePage'
import { notebookCard } from './pageObjects/notebookCard'
import downloadChecker from './downloadChecker'
import raceGamePage from './pageObjects/raceGamePage'

const start = {
  ...basicActions,
  questionGenerationService,
  testability,
  assimilation,
  assumeAssimilationPage,
  recall,
  assumeCirclePage,
  notebookCard,
  ...downloadChecker(),
  routerToRaceGamePage() {
    cy.visit('/d/race')
    cy.pageIsNotLoading()
    return raceGamePage()
  },
}
export default start
export { mock_services }
