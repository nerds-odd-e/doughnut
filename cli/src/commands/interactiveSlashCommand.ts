import type { ComponentType } from 'react'

export type InteractiveSlashCommandStageProps = {
  readonly argument?: string
  readonly onSettled: (assistantText: string) => void
}

export type InteractiveSlashCommandResult = {
  assistantMessage: string
}

export interface CommandDoc {
  readonly name: string
  readonly usage: string
  readonly description: string
}

export type InteractiveSlashCommandArgument = {
  readonly name: string
  /** When true, an empty argument is allowed (e.g. open `stageComponent` or call `run` without a value). */
  readonly optional: boolean
}

export interface InteractiveSlashCommand {
  readonly line: string
  readonly doc: CommandDoc
  /**
   * When set, a non-empty argument is required unless `optional` is true; the shell shows usage if
   * missing and not optional.
   */
  readonly argument?: InteractiveSlashCommandArgument
  readonly stageComponent?: ComponentType<InteractiveSlashCommandStageProps>
  readonly run?: (
    argument?: string
  ) => InteractiveSlashCommandResult | Promise<InteractiveSlashCommandResult>
}
