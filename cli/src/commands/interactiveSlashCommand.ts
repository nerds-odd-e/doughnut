import type { ComponentType } from 'react'

/**
 * Contract for slash-command stages: communicate results only through these props (e.g.
 * `onSettled`). Stages must not import or depend on the parent shell (`InteractiveCliApp`); the
 * shell decides what happens after completion.
 */
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
  readonly literal: string
  readonly doc: CommandDoc
  /**
   * When set, a non-empty argument is required unless `optional` is true; the shell shows usage if
   * missing and not optional.
   */
  readonly argument?: InteractiveSlashCommandArgument
  /** Renders as the active stage; must honor `InteractiveSlashCommandStageProps` isolation. */
  readonly stageComponent?: ComponentType<InteractiveSlashCommandStageProps>
  readonly run?: (
    argument?: string
  ) => InteractiveSlashCommandResult | Promise<InteractiveSlashCommandResult>
}
