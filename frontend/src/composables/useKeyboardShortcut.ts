import {
  registerShortcut,
  type ShortcutAction,
} from "@/utils/keyboardShortcuts"
import { onUnmounted, watch, type MaybeRefOrGetter, toValue } from "vue"

export function useKeyboardShortcut(
  action: ShortcutAction,
  handler: () => void,
  isActive: MaybeRefOrGetter<boolean> = true
): void {
  let unregister: (() => void) | undefined

  const sync = (active: boolean) => {
    unregister?.()
    unregister = undefined
    if (active) {
      unregister = registerShortcut(action, handler)
    }
  }

  watch(() => toValue(isActive), sync, { immediate: true })
  onUnmounted(() => {
    unregister?.()
  })
}
