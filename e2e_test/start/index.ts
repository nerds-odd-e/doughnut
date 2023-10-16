import {
  findQuestionWithStem,
  expectFeedbackRequiredMessage,
  currentQuestion,
} from "./pageObjects/QuizQuestionPage"
import { goToLastResult, answeredQuestionPage } from "./pageObjects/AnsweredQuestionPage"
import mock_services from "./mock_services"
import { questionGenerationService } from "./questionGenerationService"
import { higherOrderActions } from "./higherOrderActions"
import basicActions from "./basicActions"

const start = {
  ...basicActions,
  higherOrderActions,
  questionGenerationService,
  answeredQuestionPage,
  goToLastResult,
  findQuestionWithStem,
  currentQuestion,
  expectFeedbackRequiredMessage,
}
export default start
export { mock_services }
