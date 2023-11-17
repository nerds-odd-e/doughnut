import { questionGenerationService } from "./questionGenerationService"
import { higherOrderActions } from "./higherOrderActions"
import basicActions from "./basicActions"
import testability from "./testability"
import mock_services from "./mock_services/index"

const start = {
  ...basicActions,
  ...higherOrderActions,
  questionGenerationService,
  testability,
}
export default start
export { mock_services }
