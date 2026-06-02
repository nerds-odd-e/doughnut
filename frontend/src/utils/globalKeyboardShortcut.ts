function isModifierKeyShortcut(e: KeyboardEvent, code: string): boolean {
  if (!e.ctrlKey && !e.metaKey) return false
  if (e.shiftKey || e.altKey) return false
  return e.code === code
}

function createGlobalShortcutRegistry() {
  const openerStack: Array<() => void> = []
  return {
    register(fn: () => void) {
      openerStack.push(fn)
    },
    unregister(fn: () => void) {
      const index = openerStack.lastIndexOf(fn)
      if (index !== -1) openerStack.splice(index, 1)
    },
    hasOpener() {
      return openerStack.length > 0
    },
    open() {
      openerStack[openerStack.length - 1]?.()
    },
  }
}

const noteSearchRegistry = createGlobalShortcutRegistry()
const noteNewRegistry = createGlobalShortcutRegistry()

export function isNoteSearchShortcut(e: KeyboardEvent): boolean {
  return isModifierKeyShortcut(e, "KeyF")
}

export function isNoteNewShortcut(e: KeyboardEvent): boolean {
  return isModifierKeyShortcut(e, "KeyN")
}

export const registerGlobalNoteSearchOpener = noteSearchRegistry.register
export const unregisterGlobalNoteSearchOpener = noteSearchRegistry.unregister
export const openGlobalNoteSearch = noteSearchRegistry.open

export const registerGlobalNoteNewOpener = noteNewRegistry.register
export const unregisterGlobalNoteNewOpener = noteNewRegistry.unregister
export const hasGlobalNoteNewOpener = noteNewRegistry.hasOpener
export const openGlobalNoteNew = noteNewRegistry.open
