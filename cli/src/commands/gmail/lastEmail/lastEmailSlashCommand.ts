import { LastEmailStage } from './LastEmailStage.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
} from '../../interactiveSlashCommand.js'

const lastEmailDoc: CommandDoc = {
  name: '/last email',
  usage: '/last email',
  description: 'Show subject of last email',
}

export const lastEmailSlashCommand: InteractiveSlashCommand = {
  literal: '/last email',
  doc: lastEmailDoc,
  stageComponent: LastEmailStage,
}
