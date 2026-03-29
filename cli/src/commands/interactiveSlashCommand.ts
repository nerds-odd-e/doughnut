import type { ComponentType } from 'react'

export type InteractiveSlashCommandStageProps = {
  readonly onSettled: (assistantText: string) => void
}

export type TranscriptMessage =
  | { readonly role: 'assistant'; readonly text: string }
  | { readonly role: 'user'; readonly text: string }

export type InteractiveSlashCommandResult = {
  assistantMessage: string
  /** When set, replaces the Current guidance region (below the `> ` line). */
  currentGuidance?: string
}

export interface CommandDoc {
  readonly name: string
  readonly usage: string
  readonly description: string
}

export interface InteractiveSlashCommand {
  readonly line: string
  readonly doc: CommandDoc
  /** When set, a non-empty argument is required; InteractiveCliApp shows usage if missing. */
  readonly argumentName?: string
  readonly stageComponent?: ComponentType<InteractiveSlashCommandStageProps>
  run(
    argument?: string
  ): InteractiveSlashCommandResult | Promise<InteractiveSlashCommandResult>
}
