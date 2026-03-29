import {
  removeAccessTokenCompletely,
  removeAccessTokenLocal,
} from './accessToken.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
} from '../interactiveSlashCommand.js'

const removeAccessTokenDoc: CommandDoc = {
  name: '/remove-access-token',
  usage: '/remove-access-token <label>',
  description: 'Remove a stored access token from local config only',
}

const removeAccessTokenCompletelyDoc: CommandDoc = {
  name: '/remove-access-token-completely',
  usage: '/remove-access-token-completely <label>',
  description:
    'Revoke a stored access token on the server and remove it locally',
}

export const removeAccessTokenSlashCommand: InteractiveSlashCommand = {
  line: '/remove-access-token',
  doc: removeAccessTokenDoc,
  argumentName: 'label',
  run(argument) {
    removeAccessTokenLocal(argument!)
    return {
      assistantMessage: `Token "${argument}" removed.`,
    }
  },
}

export const removeAccessTokenCompletelySlashCommand: InteractiveSlashCommand =
  {
    line: '/remove-access-token-completely',
    doc: removeAccessTokenCompletelyDoc,
    argumentName: 'label',
    async run(argument) {
      await removeAccessTokenCompletely(argument!)
      return {
        assistantMessage: `Token "${argument}" removed locally and from server.`,
      }
    },
  }
