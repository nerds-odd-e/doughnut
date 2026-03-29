import type { ComponentType } from 'react'

export type InteractiveSlashCommandStageProps = {
  readonly onSettled: (assistantText: string) => void
}

export type TranscriptMessage =
  | { readonly role: 'assistant'; readonly text: string }
  | { readonly role: 'user'; readonly text: string }

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
   * When set, a non-empty argument is required unless `optional` is true; InteractiveCliApp shows
   * usage if missing and not optional.
   */
  readonly argument?: InteractiveSlashCommandArgument
  readonly stageComponent?: ComponentType<InteractiveSlashCommandStageProps>
  readonly run?: (
    argument?: string
  ) => InteractiveSlashCommandResult | Promise<InteractiveSlashCommandResult>
}
