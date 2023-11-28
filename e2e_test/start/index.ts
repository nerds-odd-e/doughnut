import { questionGenerationService } from "./questionGenerationService"
import { higherOrderActions } from "./higherOrderActions"
import basicActions from "./basicActions"
import testability from "./testability"
import mock_services from "./mock_services/index"
import { basicDialog } from "./basicDialog"

const start = {
  ...basicActions,
  ...higherOrderActions,
  ...basicDialog,
  questionGenerationService,
  testability,
}
export default start
export { mock_services }
