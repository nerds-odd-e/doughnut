import { Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Then(
  '{string} can see the default message {string} from message center screen',
  (user: string, message: string) => {
    start
      .reloginAndEnsureHomePage(user)
      .navigateToMessageCenter()
      .expectSingleMessage(message)
  }
)

Then(
  '{string} can see the input form and Send button when click on the question {string}',
  (user: string, question: string) => {
    start
      .reloginAndEnsureHomePage(user)
      .navigateToMessageCenter()
      .clickToSeeExpectForm(question)
  }
)
