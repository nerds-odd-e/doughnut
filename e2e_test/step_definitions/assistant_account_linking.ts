import { When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { hostedAssistant } from '../start/pageObjects/hostedAssistant'

When(
  'I connect the hosted assistant through GitHub and allow read access',
  () => {
    hostedAssistant().connectThroughGithub()
  }
)

Then(
  'the hosted assistant explores bounded graph context for {string}',
  (title: string) => {
    hostedAssistant().expectBoundedGraph(title)
  }
)

When("the hosted assistant's access expires", () => {
  hostedAssistant().waitForAccessExpiry()
})

Then(
  'the hosted assistant renews automatically and reads the same private note',
  () => {
    hostedAssistant().expectAutomaticRenewal()
  }
)

When('I withdraw the hosted assistant in Settings', () => {
  hostedAssistant().withdrawInSettings()
})

Then(
  'the existing hosted assistant connection loses access and renewal',
  () => {
    hostedAssistant().expectWithdrawal()
  }
)

When('I reconnect the hosted assistant', () => {
  hostedAssistant().reconnect()
})

Then('Donut asks for informed consent again', () => {
  hostedAssistant().expectInformedConsent()
})

Then(
  'the hosted assistant reads {string} containing {string}',
  (title: string, content: string) => {
    hostedAssistant().expectPrivateRead(title, content)
  }
)
