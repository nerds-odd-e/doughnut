<template>
  <div class="daisy-alert daisy-alert-warning" v-if="note.deletedAt">
    This note has been deleted
  </div>
  <NoteEditableTitle v-bind="{ noteTopology: note.noteTopology, readonly }" />
  <div role="details" class="note-details">
    <NoteEditableDetails
      v-bind="{
        readonly,
        noteId: note.id,
        noteDetails: note.details,
        asMarkdown,
        wikiTitles,
        relationPropertyApiNoteId: relationshipNoteRelationApiId,
      }"
      @dead-link-click="$emit('deadLinkClick', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from "vue"
import type { Note, WikiTitle } from "@generated/doughnut-backend-api"
import { detailsHasRelationProperty } from "@/utils/noteDetailsFrontmatter"
import NoteEditableTitle from "./NoteEditableTitle.vue"
import NoteEditableDetails from "./NoteEditableDetails.vue"

const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
  readonly: { type: Boolean, default: true },
  asMarkdown: Boolean,
  wikiTitles: { type: Array as PropType<WikiTitle[]>, required: true },
})

const relationshipNoteRelationApiId = computed(() =>
  detailsHasRelationProperty(props.note.details ?? "")
    ? props.note.id
    : undefined
)

defineEmits<{ deadLinkClick: [title: string] }>()
</script>
