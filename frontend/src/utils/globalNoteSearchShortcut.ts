export function isNoteSearchShortcut(e: KeyboardEvent): boolean {
  if (!e.ctrlKey && !e.metaKey) return false
  if (e.shiftKey || e.altKey) return false
  return e.code === "KeyF"
}

const openerStack: Array<() => void> = []

export function registerGlobalNoteSearchOpener(fn: () => void): void {
  openerStack.push(fn)
}

export function unregisterGlobalNoteSearchOpener(fn: () => void): void {
  const index = openerStack.lastIndexOf(fn)
  if (index !== -1) openerStack.splice(index, 1)
}

export function openGlobalNoteSearch(): void {
  openerStack[openerStack.length - 1]?.()
}
