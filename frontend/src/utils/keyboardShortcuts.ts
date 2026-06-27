import { hasOpenModal } from "@/components/commons/modalStack"

export type ShortcutAction =
  | "note-new"
  | "note-toggle-edit-mode"
  | "note-export"
  | "note-delete"
  | "note-search"
  | "note-link"

type KeyMatcher = (e: KeyboardEvent) => boolean

type ShortcutBinding = {
  matches: KeyMatcher
  guardEditable: boolean
}

function plainLetterKey(e: KeyboardEvent, code: string): boolean {
  return !e.ctrlKey && !e.metaKey && !e.shiftKey && !e.altKey && e.code === code
}

function ctrlCmdKeyF(e: KeyboardEvent, shift: boolean): boolean {
  if (!e.ctrlKey && !e.metaKey) return false
  if (e.shiftKey !== shift) return false
  if (e.altKey) return false
  return e.code === "KeyF"
}

const bindings: Record<ShortcutAction, ShortcutBinding> = {
  "note-new": {
    matches: (e) => plainLetterKey(e, "KeyN"),
    guardEditable: true,
  },
  "note-toggle-edit-mode": {
    matches: (e) => plainLetterKey(e, "KeyM"),
    guardEditable: true,
  },
  "note-export": {
    matches: (e) => plainLetterKey(e, "KeyE"),
    guardEditable: true,
  },
  "note-delete": {
    matches: (e) => plainLetterKey(e, "KeyD"),
    guardEditable: true,
  },
  "note-search": {
    matches: (e) => ctrlCmdKeyF(e, false),
    guardEditable: false,
  },
  "note-link": { matches: (e) => ctrlCmdKeyF(e, true), guardEditable: false },
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
  for (const action of Object.keys(bindings) as ShortcutAction[]) {
    const binding = bindings[action]
    if (binding.guardEditable) {
      if (hasOpenModal()) continue
      if (
        isEditableTarget(e.target) ||
        isEditableTarget(document.activeElement)
      ) {
        continue
      }
    }
    if (!binding.matches(e)) continue
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
