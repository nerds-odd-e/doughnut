import type { User } from "@generated/doughnut-backend-api"
import {
  hasGlobalNoteNewOpener,
  isNoteNewShortcut,
  isNoteSearchShortcut,
  openGlobalNoteNew,
  openGlobalNoteSearch,
} from "@/utils/globalKeyboardShortcut"
import { type Ref, watch } from "vue"

function stopKeyEvent(e: KeyboardEvent) {
  e.preventDefault()
  e.stopPropagation()
  e.stopImmediatePropagation()
}

function onDocumentKeydownCapture(e: KeyboardEvent): void {
  if (isNoteNewShortcut(e)) {
    if (!hasGlobalNoteNewOpener()) return
    stopKeyEvent(e)
    openGlobalNoteNew()
    return
  }
  if (!isNoteSearchShortcut(e)) return
  stopKeyEvent(e)
  openGlobalNoteSearch()
}

export function useGlobalKeyboardShortcuts(user: Ref<User | undefined>): void {
  watch(
    () => user.value,
    (loggedIn, _, onCleanup) => {
      if (!loggedIn) return
      document.addEventListener("keydown", onDocumentKeydownCapture, true)
      onCleanup(() =>
        document.removeEventListener("keydown", onDocumentKeydownCapture, true)
      )
    },
    { immediate: true }
  )
}
