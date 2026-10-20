<template>
  <div class="note-reading-surface">
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
  <button
    type="button"
    data-testid="note-details-toggle"
    class="note-details-toggle daisy-btn daisy-btn-ghost daisy-btn-sm mb-2 h-auto min-h-8 pointer-coarse:min-h-11 text-base-content/60 justify-start text-left font-normal"
    :aria-expanded="revealed"
    @click="revealed = !revealed"
  >
    <ChevronDown v-if="revealed" class="size-4 shrink-0" aria-hidden="true" />
    <ChevronRight v-else class="size-4 shrink-0" aria-hidden="true" />
    <span v-if="propertyKeys.length || !readonly" data-testid="note-property-summary" class="note-property-summary">
      {{ propertyKeys.length ? `Properties: ${propertyKeys.join(', ')}` : 'No properties' }}
    </span>
    <span v-if="unfinishedEdit?.()" data-testid="note-details-unfinished-edit" class="shrink-0">Unfinished edit</span>
    <span
      data-testid="note-updated-at"
      class="shrink-0"
      :data-freshness="freshness"
      :style="{ opacity: freshness / 100 }"
    >
      Updated: <time :datetime="note.noteTopology.updatedAt">{{ formatDate(note.noteTopology.updatedAt) }}</time>
    </span>
  </button>
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
    >
      <template #after-properties>
        <dl v-show="revealed" data-testid="note-detail-dates" class="mb-3 text-sm text-base-content/60">
          <div class="flex gap-1"><dt>Created:</dt><dd><time :datetime="note.noteTopology.createdAt">{{ formatDate(note.noteTopology.createdAt) }}</time></dd></div>
          <div class="flex gap-1"><dt>Updated:</dt><dd><time :datetime="note.noteTopology.updatedAt">{{ formatDate(note.noteTopology.updatedAt) }}</time></dd></div>
        </dl>
      </template>
    </NoteEditableContent>
  </div>
  </div>
</template>

<script setup lang="ts">
import { computed, provide, ref, watch, type PropType } from "vue"
import { ChevronDown, ChevronRight } from "@lucide/vue"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatter"
import { noteDetailsDisclosureKey } from "@/composables/noteDetailsDisclosure"
import type { Note, WikiLink } from "@generated/donut-backend-api"
import NoteEditableTitle from "./NoteEditableTitle.vue"
import NoteEditableContent from "./NoteEditableContent.vue"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"

const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
  readonly: { type: Boolean, default: true },
  asMarkdown: Boolean,
  wikiLinks: { type: Array as PropType<WikiLink[]>, required: true },
  isReadmeContext: { type: Boolean, default: false },
  hasInboundReferences: { type: Boolean, default: false },
  trashed: { type: Boolean, default: false },
})

defineEmits<{ deadWikiLinkClick: [payload: DeadWikiLinkPayload] }>()

const revealed = ref(false)
const unfinishedEdit = ref<() => boolean>()
const propertyKeys = computed(() => {
  const parsed = parseNoteContentMarkdown(props.note.content ?? "")
  return parsed.ok ? Object.keys(parsed.properties) : []
})
const formatDate = (date: string | undefined) =>
  date
    ? new Date(date).toLocaleDateString(undefined, {
        day: "numeric",
        month: "long",
        year: "numeric",
      })
    : ""
const freshness = computed(() => {
  const ageInHours = Math.max(
    0,
    (Date.now() - new Date(props.note.noteTopology.updatedAt!).getTime()) /
      (1000 * 60 * 60)
  )
  return Math.round(100 - Math.min(15, Math.sqrt(ageInHours)) * 3)
})
watch(
  () => props.note.id,
  () => {
    revealed.value = false
  },
  { flush: "sync" }
)
provide(noteDetailsDisclosureKey, {
  revealed,
  unfinishedEdit,
  noteId: computed(() => props.note.id),
})

const titleEditor = ref<InstanceType<typeof NoteEditableTitle> | null>(null)
defineExpose({
  beginTitleDictation: () => titleEditor.value!.beginDictation(),
})
</script>

<style scoped lang="scss">
.note-reading-surface {
  box-sizing: border-box;
  width: 100%;
  max-width: 48rem;
  min-width: 0;
  margin-inline: auto;
  padding: 1rem;
  overflow-wrap: anywhere;
}

.note-details-toggle {
  display: flex;
  flex-wrap: nowrap;
  width: 100%;
  gap: 0.375rem;
  padding-inline: 0;
  font-size: 0.75rem;
  white-space: nowrap;
}

.note-property-summary {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.note-content :deep(.ql-editor) {
  font-size: 1.0625rem;
  padding-inline: 0;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

</style>
