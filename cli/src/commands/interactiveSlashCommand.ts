export type InteractiveSlashCommandResult = {
  assistantMessage: string
}

export interface InteractiveSlashCommand {
  readonly line: string
  run(): InteractiveSlashCommandResult
}
