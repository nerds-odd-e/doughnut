<template>
  <ul v-if="(noteRealm?.children?.length ?? 0) > 0" class="list-group">
    <li
      v-for="note in noteRealm?.children"
      :key="note.id"
    >
      <SidebarNoteItem
        :note="note"
        :active-note-realm="activeNoteRealm"
        :storage-accessor="storageAccessor"
        :expanded-ids="expandedIds"
        @toggle-children="toggleChildren"
      />
    </li>
  </ul>
</template>

<script setup lang="ts">
import type { NoteRealm } from "@/generated/backend"
import type { StorageAccessor } from "../../store/createNoteStorage"
import SidebarNoteItem from "./SidebarNoteItem.vue"
import { ref, watch } from "vue"

interface Props {
  noteId: number
  activeNoteRealm: NoteRealm
  storageAccessor: StorageAccessor
}

const props = defineProps<Props>()

const noteRealm = props.storageAccessor
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(props.noteId)

const expandedIds = ref([props.activeNoteRealm.note.id])

const toggleChildren = (noteId: number) => {
  const index = expandedIds.value.indexOf(noteId)
  if (index === -1) {
    expandedIds.value.push(noteId)
  } else {
    expandedIds.value.splice(index, 1)
  }
}

watch(
  () => props.activeNoteRealm.note.noteTopic.parentNoteTopic,
  (parentNoteTopic) => {
    const uniqueIds = new Set([
      ...expandedIds.value,
      props.activeNoteRealm.note.id,
    ])
    let cursor = parentNoteTopic
    while (cursor) {
      uniqueIds.add(cursor.id)
      cursor = cursor.parentNoteTopic
    }
    expandedIds.value = Array.from(uniqueIds)
  },
  { immediate: true }
)
</script>
