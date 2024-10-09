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
