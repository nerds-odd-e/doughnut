export interface CommandDoc {
  name: string
  usage: string
  description: string
  category: 'subcommand' | 'interactive'
}

/** Slash commands shown in the TTY (cyan prefix highlight only; no /help or completion UI). */
export const interactiveDocs: CommandDoc[] = [
  {
    name: '/exit',
    usage: '/exit',
    description: 'Quit the CLI',
    category: 'interactive',
  },
]
