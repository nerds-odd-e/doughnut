import basicActions from './basicActions'
import { higherOrderActions } from './higherOrderActions'
import mock_services from './mock_services/index'
import { questionGenerationService } from './questionGenerationService'
import testability from './testability'
import { assimilation } from './pageObjects/assimilationPage'
import { recall } from './pageObjects/recallPage'
import { assumeCirclePage } from './pageObjects/circlePage'

const start = {
  ...basicActions,
  ...higherOrderActions,
  questionGenerationService,
  testability,
  assimilation,
  recall,
  assumeCirclePage,
}
export default start
export { mock_services }
