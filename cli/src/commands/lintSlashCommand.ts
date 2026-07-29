import { lintWorkspace } from '../lint/lintWorkspace.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
} from './interactiveSlashCommand.js'

const lintDoc: CommandDoc = {
  name: '/lint',
  usage: '/lint <workspace directory>',
  description:
    'Check whether a local Markdown workspace follows the Open Knowledge Format',
}

export const lintSlashCommand: InteractiveSlashCommand = {
  literal: '/lint',
  doc: lintDoc,
  argument: { name: 'workspace directory', optional: false },
  run: (argument) => ({ assistantMessage: lintWorkspace(argument ?? '') }),
}
