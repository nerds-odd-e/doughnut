<template>
  <nav
    v-if="!readonly"
    class="daisy-navbar daisy-bg-base-200 daisy-min-h-0 daisy-py-1 daisy-px-1 daisy-sticky daisy-top-0 daisy-z-10"
  >
    <div class="daisy-btn-group daisy-btn-group-sm">
      <NoteNewButton
        button-title="Add Child Note"
        v-bind="{ referenceNote: note, insertMode: 'as-child' }"
      >
        <FolderPlus class="w-5 h-5" />
      </NoteNewButton>
      <NoteNewButton
        v-if="note.parentId"
        button-title="Add Next Sibling Note"
        v-bind="{ referenceNote: note, insertMode: 'after' }"
      >
        <Folders class="w-5 h-5" />
      </NoteNewButton>
    </div>
  </nav>
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import { FolderPlus, Folders } from "lucide-vue-next"
import NoteNewButton from "./core/NoteNewButton.vue"

defineProps<{
  note: Note
  readonly?: boolean
}>()
</script>
