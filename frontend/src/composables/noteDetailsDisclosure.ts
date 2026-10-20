import { inject, onScopeDispose, type InjectionKey, type Ref } from "vue"

export interface NoteDetailsDisclosure {
  revealed: Ref<boolean>
  noteId: Ref<number>
  unfinishedEdit: Ref<(() => boolean) | undefined>
}

export const noteDetailsDisclosureKey: InjectionKey<NoteDetailsDisclosure> =
  Symbol("noteDetailsDisclosure")

export function useNoteDetailsDisclosure(unfinishedEdit?: () => boolean) {
  const disclosure = inject(noteDetailsDisclosureKey, undefined)
  if (disclosure && unfinishedEdit) {
    disclosure.unfinishedEdit.value = unfinishedEdit
    onScopeDispose(() => {
      if (disclosure.unfinishedEdit.value === unfinishedEdit) {
        disclosure.unfinishedEdit.value = undefined
      }
    })
  }
  return disclosure
}
