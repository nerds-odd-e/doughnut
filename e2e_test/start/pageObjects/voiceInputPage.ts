import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  clickToolbarOverflowAction,
  noteToolbar,
  visibleToolbarActionButton,
} from './noteToolbarOverflow'

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
        .find('button[title="Voice input"]', { timeout: 30000 })
        .should('be.enabled')
      waitUntilAppIsNotBusy()
      return this
    },
  }
}

export default voiceInputPage
