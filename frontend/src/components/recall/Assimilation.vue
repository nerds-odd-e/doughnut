<template>
  <main class="assimilation-main">
    <NoteShow
      v-bind="{
        noteId: note.id,
        expandChildren: false,
        showBreadcrumb: true,
        ancestorFolders,
      }"
    />
  </main>
  <AssimilationPanel
    :key="note.id"
    :note="note"
    @reload-needed="emit('reloadNeeded')"
    @assimilation-done="emit('assimilationDone')"
  />
</template>

<script setup lang="ts">
import type { Folder, Note } from "@generated/doughnut-backend-api"
import AssimilationPanel from "./AssimilationPanel.vue"
import NoteShow from "../notes/NoteShow.vue"

const { note, ancestorFolders = [] } = defineProps<{
  note: Note
  ancestorFolders?: Folder[]
}>()

const emit = defineEmits<{
  (e: "reloadNeeded"): void
  (e: "assimilationDone"): void
}>()
</script>

<style scoped lang="scss">
@use "@/assets/menu-variables.scss" as *;

/* Space for fixed settings dock only when that dock is active (see AssimilationSettings). */
.assimilation-main {
  padding-bottom: 1rem;
}

@media (min-height: $assimilation-dock-min-height) {
  .assimilation-main {
    padding-bottom: clamp(11rem, 32vh, 22rem);
  }
}
</style>
