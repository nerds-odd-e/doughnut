import { inject, provide, ref, type InjectionKey, type Ref } from "vue"

export const notebookSidebarOpenedKey: InjectionKey<Ref<boolean>> = Symbol(
  "notebookSidebarOpened"
)

export function provideNotebookSidebarOpened(opened: Ref<boolean>) {
  provide(notebookSidebarOpenedKey, opened)
}

export function useNotebookSidebarOpened(): Ref<boolean> {
  return inject(notebookSidebarOpenedKey, ref(true))
}
