import { notebookSidebarOpenedKey } from "@/composables/notebookSidebarOpened"
import type { App } from "vue"
import { ref, type Ref } from "vue"

export function notebookSidebarOpenedPlugin(opened: Ref<boolean>) {
  return {
    install(app: App) {
      app.provide(notebookSidebarOpenedKey, opened)
    },
  }
}

export function notebookSidebarClosedPlugin() {
  return notebookSidebarOpenedPlugin(ref(false))
}
