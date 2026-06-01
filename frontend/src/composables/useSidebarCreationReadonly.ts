import type { NoteRealm, User } from "@generated/doughnut-backend-api"
import { computed, type MaybeRefOrGetter, toValue, type Ref } from "vue"

export function useSidebarCreationReadonly(
  currentUser: Ref<User | undefined> | undefined,
  props: MaybeRefOrGetter<{
    activeNoteRealm?: NoteRealm
    notebookReadonly?: boolean
  }>
) {
  return computed(() => {
    if (!currentUser?.value) return true
    const { activeNoteRealm, notebookReadonly } = toValue(props)
    const realmReadonly = activeNoteRealm?.notebookRealm.readonly
    if (realmReadonly === true) return true
    if (activeNoteRealm != null) return false
    return notebookReadonly === true
  })
}
