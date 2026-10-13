import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  EXCLUDED_ATTEMPT_NOTICE,
  expectExcludedAsAmbiguous,
} from './excludedAttemptNotice'

const helpDialog = () => cy.get('dialog:contains("Private recall help")')

export const outsidePrivateRecallHelp = (selector: string) =>
  cy.get(selector).not('dialog *')

export const assumePrivateRecallHelp = () => {
  const self = {
    ask(message: string) {
      helpDialog().find('#message-input').type(message, { delay: 0 })
      helpDialog()
        .findByRole('button', { name: 'Send message and invite AI to reply' })
        .click()
      waitUntilAppIsNotBusy()
      return self
    },
    confirmExclusionProposal(reason: string) {
      helpDialog()
        .find('.ai-assistant')
        .should(
          'contain.text',
          'Proposed correction: exclude this attempt as an ambiguous question'
        )
        .find('.exclusion-reason')
        .should('contain.text', reason)
      helpDialog().findByRole('button', { name: 'Confirm' }).click()
      waitUntilAppIsNotBusy()
      return self
    },
    expectAttemptExcludedAsAmbiguous(reason: string) {
      expectExcludedAsAmbiguous(
        helpDialog().find(EXCLUDED_ATTEMPT_NOTICE),
        reason,
        'private help subject'
      )
      return self
    },
    close() {
      helpDialog().findByRole('button', { name: 'Close dialog' }).click()
      helpDialog().should('not.exist')
    },
  }
  return self
}

export const openPrivateRecallHelp = () => {
  cy.findByRole('button', { name: 'Get private help' }).click()
  waitUntilAppIsNotBusy()
  return assumePrivateRecallHelp()
}
