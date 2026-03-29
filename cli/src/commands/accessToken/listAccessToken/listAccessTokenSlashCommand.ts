import { ListAccessTokenStage } from './ListAccessTokenStage.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
} from '../../interactiveSlashCommand.js'

const listAccessTokenDoc: CommandDoc = {
  name: '/list-access-token',
  usage: '/list-access-token',
  description: 'List stored Doughnut API access tokens',
}

export const listAccessTokenSlashCommand: InteractiveSlashCommand = {
  line: '/list-access-token',
  doc: listAccessTokenDoc,
  stageComponent: ListAccessTokenStage,
}
