import { provideNoteShortcutScope } from "@/composables/noteShortcutScope"
import { defineComponent, h, type Component } from "vue"

export function wrapWithNoteShortcutScope(
  component: Component,
  props: Record<string, unknown>,
  active: boolean
) {
  return defineComponent({
    setup() {
      provideNoteShortcutScope(active)
      return () => h(component, props)
    },
  })
}
