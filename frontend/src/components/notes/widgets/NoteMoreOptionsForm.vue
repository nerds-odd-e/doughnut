<template>
  <DropdownMenu size="wide">
    <NoteMoreOptionsActions
      layout="menu"
      v-bind="{ note }"
      @close-dialog="$emit('close-dialog')"
    />
  </DropdownMenu>
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import DropdownMenu from "@/components/commons/DropdownMenu.vue"
import NoteMoreOptionsActions from "./NoteMoreOptionsActions.vue"

defineProps<{
  note: Note
}>()

defineEmits<{
  (e: "close-dialog"): void
}>()
</script>
