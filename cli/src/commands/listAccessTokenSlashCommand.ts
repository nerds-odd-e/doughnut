import { DEFAULT_INTERACTIVE_GUIDANCE } from '../interactiveGuidanceDefault.js'
import {
  formatNumberedListForTerminal,
  resolvedTerminalWidth,
} from '../terminalColumns.js'
import { getStoredAccessTokenLabels } from './accessToken.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
} from './interactiveSlashCommand.js'

const listAccessTokenDoc: CommandDoc = {
  name: '/list-access-token',
  usage: '/list-access-token',
  description: 'List stored Doughnut API access tokens',
}

export const listAccessTokenSlashCommand: InteractiveSlashCommand = {
  line: '/list-access-token',
  doc: listAccessTokenDoc,
  run() {
    const labels = getStoredAccessTokenLabels()
    if (labels.length === 0) {
      return {
        assistantMessage: 'No access tokens stored.',
        currentGuidance: DEFAULT_INTERACTIVE_GUIDANCE,
      }
    }
    return {
      assistantMessage: 'Stored access tokens:',
      currentGuidance: formatNumberedListForTerminal(
        labels,
        resolvedTerminalWidth()
      ),
    }
  },
}
