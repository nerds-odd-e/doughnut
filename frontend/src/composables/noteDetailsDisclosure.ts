import type { InjectionKey, Ref } from "vue"

export interface NoteDetailsDisclosure {
  revealed: Ref<boolean>
  panelId: string
  noteId: Ref<number>
}

export const noteDetailsDisclosureKey: InjectionKey<NoteDetailsDisclosure> =
  Symbol("noteDetailsDisclosure")
