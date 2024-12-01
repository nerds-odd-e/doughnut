import basicActions from './basicActions'
import { higherOrderActions } from './higherOrderActions'
import mock_services from './mock_services/index'
import { questionGenerationService } from './questionGenerationService'
import testability from './testability'
import { assimilation } from './pageObjects/assimilationPage'

const start = {
  ...basicActions,
  ...higherOrderActions,
  questionGenerationService,
  testability,
  assimilation,
}
export default start
export { mock_services }
