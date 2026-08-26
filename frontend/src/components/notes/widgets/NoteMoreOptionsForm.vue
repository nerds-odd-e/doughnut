<template>
  <DropdownMenu size="wide">
    <NoteMoreOptionsActions
      layout="menu"
      v-bind="{ note }"
      :only="only"
      :as-markdown="asMarkdown"
      @close-dialog="$emit('close-dialog')"
      @edit-as-markdown="$emit('edit-as-markdown', $event)"
      @open-wiki="$emit('open-wiki')"
      @open-new="$emit('open-new')"
    />
  </DropdownMenu>
</template>

<script setup lang="ts">
import type { Note } from "@generated/donut-backend-api"
import DropdownMenu from "@/components/commons/DropdownMenu.vue"
import NoteMoreOptionsActions from "./NoteMoreOptionsActions.vue"
import type { NoteMoreOptionsActionId } from "./noteMoreOptionsTitles"

defineProps<{
  note: Note
  only?: NoteMoreOptionsActionId[]
  asMarkdown?: boolean
}>()

defineEmits<{
  (e: "close-dialog"): void
  (e: "edit-as-markdown", value: boolean): void
  (e: "open-wiki"): void
  (e: "open-new"): void
}>()
</script>
