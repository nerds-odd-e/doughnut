<template>
  <div class="daisy-alert daisy-alert-warning" v-if="note.deletedAt">
    This note has been deleted
  </div>
  <NoteEditableTitle v-bind="{ noteTopology: note.noteTopology, readonly }" />
  <div
    role="region"
    aria-label="Note content"
    class="note-content"
  >
    <NoteEditableContent
      v-bind="{
        readonly,
        noteId: note.id,
        noteContent: note.content,
        asMarkdown,
        wikiTitles,
        noteTitleForWikidataSearch: note.noteTopology.title,
        isIndexContext,
      }"
      @dead-link-click="$emit('deadLinkClick', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import { type PropType } from "vue"
import type { Note, WikiTitle } from "@generated/doughnut-backend-api"
import NoteEditableTitle from "./NoteEditableTitle.vue"
import NoteEditableContent from "./NoteEditableContent.vue"
import type { DeadLinkPayload } from "@/utils/wikiPropertyValueField"

const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
  readonly: { type: Boolean, default: true },
  asMarkdown: Boolean,
  wikiTitles: { type: Array as PropType<WikiTitle[]>, required: true },
  isIndexContext: { type: Boolean, default: false },
})

defineEmits<{ deadLinkClick: [payload: DeadLinkPayload] }>()
</script>
