import basicActions from './basicActions'
import { higherOrderActions } from './higherOrderActions'
import mock_services from './mock_services/index'
import { questionGenerationService } from './questionGenerationService'
import testability from './testability'
import { assimilation } from './pageObjects/assimilationPage'
import { recall } from './pageObjects/recallPage'

const start = {
  ...basicActions,
  ...higherOrderActions,
  questionGenerationService,
  testability,
  assimilation,
  recall,
}
export default start
export { mock_services }
