/** Shared ANSI strip for PTY transcripts (Node tasks + Cypress page objects). */
export function stripAnsiCliPty(text: string): string {
  const esc = `${String.fromCharCode(0x1b)}${String.fromCharCode(0x9b)}`
  const pattern = new RegExp(
    `[${esc}][[\\]()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-ORZcf-nqry=><]`,
    'g'
  )
  return text.replace(pattern, '')
}
