<template>
  <PopButton :title="buttonTitle">
    <template #button_face>
      <slot />
    </template>
    <template #default="{ closer }">
      <NoteNewDialog
        v-bind="{ referenceNote, insertMode }"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import type { Note } from "@generated/backend"
import type { InsertMode } from "@/models/InsertMode"
import PopButton from "../../commons/Popups/PopButton.vue"
import NoteNewDialog from "../NoteNewDialog.vue"

defineProps<{
  referenceNote: Note
  insertMode: InsertMode
  buttonTitle: string
}>()
</script>
