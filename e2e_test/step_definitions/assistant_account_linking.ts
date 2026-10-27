import { When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { hostedAssistant } from '../start/pageObjects/hostedAssistant'

When(
  'I connect the hosted assistant through GitHub and allow read access',
  () => {
    hostedAssistant().connectThroughGithub()
  }
)

Then(
  'the hosted assistant reads {string} containing {string}',
  (title: string, content: string) => {
    hostedAssistant().expectPrivateRead(title, content)
  }
)
