export type InteractiveSlashCommandResult = {
  assistantMessage: string
}

export interface CommandDoc {
  readonly name: string
  readonly usage: string
  readonly description: string
  readonly category: 'subcommand' | 'interactive'
}

export interface InteractiveSlashCommand {
  readonly line: string
  readonly doc: CommandDoc
  run(): InteractiveSlashCommandResult
}
