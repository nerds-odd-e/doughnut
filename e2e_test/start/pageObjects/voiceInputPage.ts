import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  clickToolbarOverflowAction,
  noteToolbar,
  visibleToolbarActionButton,
} from './noteToolbarOverflow'

const retryTitle = 'Retry turning your speech into text'

const voiceInputPage = () => {
  return {
    startRecording() {
      clickToolbarOverflowAction('Voice input')
      visibleToolbarActionButton('Stop voice input').should('be.visible')
      return this
    },
    stopRecording() {
      visibleToolbarActionButton('Stop voice input').click()
      noteToolbar()
        .find(
          `button[title="Voice input"]:enabled, button[title="${retryTitle}"]`,
          { timeout: 30000 }
        )
        .should('exist')
      waitUntilAppIsNotBusy()
      return this
    },
    retryConvertingSpeech() {
      visibleToolbarActionButton(retryTitle).click()
      noteToolbar()
        .find('button[title="Voice input"]', { timeout: 30000 })
        .should('be.enabled')
      waitUntilAppIsNotBusy()
      return this
    },
  }
}

export default voiceInputPage
