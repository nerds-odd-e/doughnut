<template>
  <li
    class="daisy-list-group-item daisy-list-group-item-action daisy-py-2 daisy-pb-0 daisy-pe-0 daisy-border-0"
    :class="{
      'active-item':
        activeNoteRealm != null &&
        noteRealm.id === activeNoteRealm.note.id,
    }"
    @click="onNoteRowClick"
  >
    <div
      class="daisy-flex daisy-w-full daisy-justify-between daisy-items-start note-content"
    >
      <NoteTitleWithLink
        :class="{
          'active-title':
            activeNoteRealm != null &&
            noteRealm.id === activeNoteRealm.note.id,
        }"
        v-bind="{ noteTopology: noteRealm.note.noteTopology }"
      />
      <ScrollTo v-if="activeNoteRealm != null && noteRealm.id === activeNoteRealm.note.id" />
    </div>
  </li>
</template>

<script setup lang="ts">
import type { Note, NoteRealm } from "@generated/doughnut-backend-api"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import NoteTitleWithLink from "./NoteTitleWithLink.vue"
import { sidebarUserActiveFolderIdKey } from "./sidebarFolderExpansion"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { inject } from "vue"

const storageAccessor = useStorageAccessor()
const userActiveFolderId = inject(sidebarUserActiveFolderIdKey, undefined)

interface Props {
  note: Note
  activeNoteRealm?: NoteRealm
}

const props = defineProps<Props>()

const noteRealm = storageAccessor.value.refOfNoteRealmWithFallback(props.note)

function onNoteRowClick() {
  if (userActiveFolderId != null) {
    userActiveFolderId.value = null
  }
}
</script>

<style lang="scss" scoped>
.active-item {
  border-left: 1px solid gray !important;
}

.active-title {
  font-weight: bold;
}

.list-group-item {
  position: relative;
  border-radius: 0 !important;
  min-height: 24px;
}

.note-content {
  position: relative;
  padding-bottom: 4px;
}
</style>
