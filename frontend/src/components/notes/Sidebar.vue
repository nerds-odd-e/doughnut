<template>
  <div class="daisy-ml-[-1rem]">
    <NoteSidebarToolbar
      v-if="activeNoteRealm && headNoteId"
      :note="activeNoteRealm.note"
      :readonly="sidebarReadonly"
    />
    <SidebarInner
      :class="{ 'is-disabled': !activeNoteRealm }"
      v-if="activeNoteRealm && headNoteId"
      v-bind="{
        noteId: headNoteId,
        activeNoteRealm: activeNoteRealm,
      }"
      :key="headNoteId"
    />
  </div>
</template>

<script setup lang="ts">
import type { PropType, Ref } from "vue"
import { computed, inject } from "vue"
import type { NoteRealm, User } from "@generated/doughnut-backend-api"
import NoteSidebarToolbar from "./NoteSidebarToolbar.vue"
import SidebarInner from "./SidebarInner.vue"

const props = defineProps({
  activeNoteRealm: { type: Object as PropType<NoteRealm> },
})

const currentUser = inject<Ref<User | undefined>>("currentUser")
const sidebarReadonly = computed(
  () =>
    !props.activeNoteRealm ||
    !currentUser?.value ||
    props.activeNoteRealm.fromBazaar === true
)

const headNoteId = computed(() => {
  if (!props.activeNoteRealm) return undefined
  let cursor = props.activeNoteRealm.note.noteTopology
  while (cursor.parentOrSubjectNoteTopology) {
    cursor = cursor.parentOrSubjectNoteTopology
  }
  return cursor.id
})
</script>

<style scoped>
.is-disabled {
  opacity: 0.5;
  pointer-events: none;
}
</style>
