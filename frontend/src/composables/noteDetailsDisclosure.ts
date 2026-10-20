import type { InjectionKey, Ref } from "vue"

export interface NoteDetailsDisclosure {
  revealed: Ref<boolean>
}

export const noteDetailsDisclosureKey: InjectionKey<NoteDetailsDisclosure> =
  Symbol("noteDetailsDisclosure")
