<template>
  <div class="daisy-alert daisy-alert-warning" v-if="note.deletedAt">
    This note has been deleted
  </div>
  <NoteEditableTitle
    v-bind="{
      noteTopology: note.noteTopology,
      noteId: note.id,
      readonly,
      hasInboundReferences,
    }"
  />
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
        wikiLinks,
        noteTitleForWikidataSearch: note.noteTopology.title,
        isReadmeContext,
      }"
      @dead-wiki-link-click="$emit('deadWikiLinkClick', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import { type PropType } from "vue"
import type { Note, WikiLink } from "@generated/donut-backend-api"
import NoteEditableTitle from "./NoteEditableTitle.vue"
import NoteEditableContent from "./NoteEditableContent.vue"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"

defineProps({
  note: { type: Object as PropType<Note>, required: true },
  readonly: { type: Boolean, default: true },
  asMarkdown: Boolean,
  wikiLinks: { type: Array as PropType<WikiLink[]>, required: true },
  isReadmeContext: { type: Boolean, default: false },
  hasInboundReferences: { type: Boolean, default: false },
})

defineEmits<{ deadWikiLinkClick: [payload: DeadWikiLinkPayload] }>()
</script>
