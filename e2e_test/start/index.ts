import {
  findQuestionWithStem,
  expectFeedbackRequiredMessage,
  currentQuestion,
} from "./pageObjects/QuizQuestionPage"
import mock_services from "./mock_services"
import { questionGenerationService } from "./questionGenerationService"
import { higherOrderActions } from "./higherOrderActions"
import basicActions from "./basicActions"

const start = {
  ...basicActions,
  higherOrderActions,
  questionGenerationService,
  findQuestionWithStem,
  currentQuestion,
  expectFeedbackRequiredMessage,
}
export default start
export { mock_services }
