<template>
  <div class="note-reading-column py-4">
  <div
    class="daisy-alert daisy-alert-warning"
    data-testid="note-availability-warning"
    v-if="trashed"
  >
    This note is in trash
  </div>
  <NoteEditableTitle
    ref="titleEditor"
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
  </div>
</template>

<script setup lang="ts">
import { ref, type PropType } from "vue"
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
  trashed: { type: Boolean, default: false },
})

defineEmits<{ deadWikiLinkClick: [payload: DeadWikiLinkPayload] }>()

const titleEditor = ref<InstanceType<typeof NoteEditableTitle> | null>(null)
defineExpose({
  beginTitleDictation: () => titleEditor.value!.beginDictation(),
})
</script>

<style scoped lang="scss">
.note-content :deep(.ql-editor) {
  font-size: 1.0625rem;
  padding-inline: 0;
  line-height: 1.55;
  overflow-wrap: anywhere;
}
</style>
