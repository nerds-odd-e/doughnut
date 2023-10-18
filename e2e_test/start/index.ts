import mock_services from "./mock_services"
import { questionGenerationService } from "./questionGenerationService"
import { higherOrderActions } from "./higherOrderActions"
import basicActions from "./basicActions"

const start = {
  ...basicActions,
  ...higherOrderActions,
  questionGenerationService,
}
export default start
export { mock_services }
