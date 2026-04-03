import type { ComponentType } from 'react'
import type {
  InteractiveSlashCommand,
  InteractiveSlashCommandStageProps,
} from './interactiveSlashCommand.js'
import type { ResolvedInteractiveSlashCommand } from './interactiveSlashCommands.js'
import { userVisibleSlashCommandError } from '../userVisibleSlashCommandError.js'

export type InteractiveRunSlashCommand = Extract<
  InteractiveSlashCommand,
  { run: (argument?: string) => unknown }
>

export type OpenSlashStageParams = {
  readonly component: ComponentType<InteractiveSlashCommandStageProps>
  readonly stageIndicator?: string
}

export type ApplyResolvedInteractiveSlashCommandHandlers = {
  readonly appendScrollbackError: (message: string) => void
  readonly setStageArgumentRef: (argument: string | undefined) => void
  readonly openStage: (params: OpenSlashStageParams) => void
  readonly onRunSuccess: (
    command: InteractiveRunSlashCommand,
    assistantMessage: string
  ) => void
}

/**
 * Shared slash commit pipeline: validate args, open a stage, or run a command.
 * Callers append the user line to scrollback before invoking this.
 */
export function applyResolvedInteractiveSlashCommand(
  resolved: ResolvedInteractiveSlashCommand,
  handlers: ApplyResolvedInteractiveSlashCommandHandlers
): void {
  const { command, argument } = resolved
  if ('stageComponent' in command) {
    const argumentMissing = argument === undefined || argument === ''
    const argSpec = command.argument
    if (argSpec !== undefined && argumentMissing && !argSpec.optional) {
      handlers.appendScrollbackError(
        `Missing ${argSpec.name}. Usage: ${command.doc.usage}`
      )
      return
    }
    handlers.setStageArgumentRef(argument)
    const indicator = command.stageIndicator
    handlers.openStage({
      component: command.stageComponent,
      stageIndicator:
        indicator !== undefined && indicator !== '' ? indicator : undefined,
    })
    return
  }
  const argumentMissing = argument === undefined || argument === ''
  const argSpec = command.argument
  if (argSpec !== undefined && argumentMissing && !argSpec.optional) {
    handlers.appendScrollbackError(
      `Missing ${argSpec.name}. Usage: ${command.doc.usage}`
    )
    return
  }
  Promise.resolve(command.run(argument))
    .then((r) => {
      handlers.onRunSuccess(command, r.assistantMessage)
    })
    .catch((err: unknown) => {
      handlers.appendScrollbackError(userVisibleSlashCommandError(err))
    })
}
