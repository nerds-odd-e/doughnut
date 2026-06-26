export type ShortcutAction = "note-new" | "note-toggle-edit-mode"

type KeyMatcher = (e: KeyboardEvent) => boolean

function plainLetterKey(e: KeyboardEvent, code: string): boolean {
  return !e.ctrlKey && !e.metaKey && !e.shiftKey && !e.altKey && e.code === code
}

const bindings: Record<ShortcutAction, KeyMatcher> = {
  "note-new": (e) => plainLetterKey(e, "KeyN"),
  "note-toggle-edit-mode": (e) => plainLetterKey(e, "KeyM"),
}

const handlerStacks = new Map<ShortcutAction, Array<() => void>>()

let listenerInstalled = false

function isEditableTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false
  const tag = target.tagName
  if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return true
  return target.isContentEditable
}

function onDocumentKeydownCapture(e: KeyboardEvent): void {
  if (isEditableTarget(e.target) || isEditableTarget(document.activeElement)) {
    return
  }

  for (const action of Object.keys(bindings) as ShortcutAction[]) {
    if (!bindings[action](e)) continue
    const stack = handlerStacks.get(action)
    const handler = stack?.[stack.length - 1]
    if (!handler) continue
    e.preventDefault()
    e.stopImmediatePropagation()
    handler()
    return
  }
}

function ensureListener(): void {
  if (listenerInstalled) return
  document.addEventListener("keydown", onDocumentKeydownCapture, true)
  listenerInstalled = true
}

function removeListenerIfIdle(): void {
  for (const stack of handlerStacks.values()) {
    if (stack.length > 0) return
  }
  if (!listenerInstalled) return
  document.removeEventListener("keydown", onDocumentKeydownCapture, true)
  listenerInstalled = false
}

export function registerShortcut(
  action: ShortcutAction,
  handler: () => void
): () => void {
  let stack = handlerStacks.get(action)
  if (!stack) {
    stack = []
    handlerStacks.set(action, stack)
  }
  stack.push(handler)
  ensureListener()

  return () => {
    const current = handlerStacks.get(action)
    if (!current) return
    const index = current.lastIndexOf(handler)
    if (index !== -1) current.splice(index, 1)
    removeListenerIfIdle()
  }
}
