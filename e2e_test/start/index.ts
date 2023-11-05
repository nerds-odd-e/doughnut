import mock_services from "./mock_services"
import { questionGenerationService } from "./questionGenerationService"
import { higherOrderActions } from "./higherOrderActions"
import basicActions from "./basicActions"
import testability from "./testability"

const start = {
  ...basicActions,
  ...higherOrderActions,
  questionGenerationService,
  testability,
}
export default start
export { mock_services }
