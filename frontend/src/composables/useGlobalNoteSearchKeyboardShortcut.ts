import type { User } from "@generated/doughnut-backend-api"
import {
  isNoteSearchShortcut,
  openGlobalNoteSearch,
} from "@/utils/globalNoteSearchShortcut"
import { type Ref, watch } from "vue"

function onDocumentKeydownCapture(e: KeyboardEvent): void {
  if (!isNoteSearchShortcut(e)) return
  e.preventDefault()
  e.stopPropagation()
  e.stopImmediatePropagation()
  openGlobalNoteSearch()
}

export function useGlobalNoteSearchKeyboardShortcut(
  user: Ref<User | undefined>
): void {
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
