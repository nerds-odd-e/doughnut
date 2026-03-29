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
  run(): InteractiveSlashCommandResult | Promise<InteractiveSlashCommandResult>
}
