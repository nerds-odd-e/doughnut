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
  'I can see the input form and Send button when click on the question {string}',
  (question: string) => {
    start.assumeMessageCenterPage().clickToSeeExpectForm(question)
  }
)

Then(
  'I can type the message {string} and send this message to conversation room',
  (message: string) => {
    start.assumeMessageCenterPage().typeAndSendMessage(message)
  }
)
