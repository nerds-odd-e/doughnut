import { LastEmailStage } from '../LastEmailStage.js'
import { runLastEmailInteractiveAssistantMessage } from './gmail.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
} from './interactiveSlashCommand.js'

const lastEmailDoc: CommandDoc = {
  name: '/last email',
  usage: '/last email',
  description: 'Show subject of last email',
}

export const lastEmailSlashCommand: InteractiveSlashCommand = {
  line: '/last email',
  doc: lastEmailDoc,
  stageComponent: LastEmailStage,
  async run() {
    return {
      assistantMessage: await runLastEmailInteractiveAssistantMessage(),
    }
  },
}
