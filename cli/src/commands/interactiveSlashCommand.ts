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

export interface InteractiveSlashCommand {
  readonly line: string
  readonly doc: CommandDoc
  readonly stageComponent?: ComponentType<InteractiveSlashCommandStageProps>
  /**
   * When the committed line is not exactly `line` (e.g. `/cmd arg…`), return true so
   * resolution can still dispatch here after the exact `line` map miss.
   */
  readonly matchesCommittedLine?: (line: string) => boolean
  run(
    committedLine?: string
  ): InteractiveSlashCommandResult | Promise<InteractiveSlashCommandResult>
}
