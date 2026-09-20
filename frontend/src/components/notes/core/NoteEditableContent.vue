<template>
  <div ref="rootRef" class="note-editable-content">
    <TextContentWrapper
      :value="noteContent"
      field="edit content"
      :before-save-content="beforeSaveContent"
    >
      <template #default="{ value, update, blur }">
        <TextArea
          v-if="asMarkdown"
          ref="textareaRef"
          :multiple-line="true"
          scope-name="note"
          :model-value="value"
          :readonly="readonly"
          :auto-extend-until="1000"
          @update:model-value="handleTextareaModelUpdate(update, $event)"
          @blur="blur"
          @paste="(event) => handleTextareaPaste(event, value, update)"
          @click="captureTextareaSelection"
          @keyup="captureTextareaSelection"
          @mouseup="captureTextareaSelection"
          @focus="captureTextareaSelection"
        />
        <RichMarkdownEditor
          v-else
          ref="richEditorRef"
          :multiple-line="true"
          scope-name="note"
          :model-value="value"
          :readonly="readonly"
          :wiki-links="wikiLinks"
          :last-saved-markdown="noteContent ?? ''"
          :note-title-for-wikidata-search="noteTitleForWikidataSearch"
          :note-id="noteId"
          :is-readme-context="isReadmeContext"
          @update:model-value="update(noteId, $event)"
          @blur="blur"
          @paste-complete="
            (content, quillContext) =>
              handlePasteComplete(content, update, quillContext)
          "
          @dead-wiki-link-click="emit('deadWikiLinkClick', $event)"
        />
      </template>
    </TextContentWrapper>
    <div
      v-if="pasteChoice"
      class="paste-choice mt-1 flex items-center gap-1 text-sm"
      data-testid="paste-choice"
      @mouseenter="pausePasteChoiceExpiry"
      @mouseleave="resumePasteChoiceExpiry"
      @focusin="pausePasteChoiceExpiry"
      @focusout="resumePasteChoiceExpiry"
    >
      <button
        type="button"
        class="daisy-btn daisy-btn-ghost daisy-btn-xs"
        data-testid="paste-choice-action"
        @mousedown.prevent
        @click="pasteChoice.replace()"
      >
        Use original text
      </button>
      <button
        type="button"
        class="daisy-btn daisy-btn-ghost daisy-btn-xs daisy-btn-square"
        data-testid="paste-choice-dismiss"
        aria-label="Dismiss"
        @mousedown.prevent
        @click="clearPasteChoice"
      >
        <X :size="14" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { X } from "@lucide/vue"
import { nextTick, onMounted, onUnmounted, ref, type PropType } from "vue"
import RichMarkdownEditor from "../../form/RichMarkdownEditor.vue"
import TextContentWrapper from "./TextContentWrapper.vue"
import TextArea from "@/components/form/TextArea.vue"
import type { WikiLink } from "@generated/donut-backend-api"
import { useContentCursorInserter } from "@/composables/useContentCursorInserter"
import { useNoteContentPaste } from "@/composables/useNoteContentPaste"
import { usePropertyMemoryTrackerGuard } from "@/composables/usePropertyMemoryTrackerGuard"
import {
  appendWikiLinkPropertyRow,
  diffFrontmatterPropertyKeyChanges,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"

const emit = defineEmits<{
  deadWikiLinkClick: [payload: DeadWikiLinkPayload]
}>()

const props = defineProps({
  noteId: { type: Number, required: true },
  noteContent: { type: String, required: false },
  readonly: { type: Boolean, default: true },
  asMarkdown: Boolean,
  wikiLinks: { type: Array as PropType<WikiLink[]>, required: true },
  noteTitleForWikidataSearch: { type: String, default: "" },
  isReadmeContext: { type: Boolean, default: false },
})

const propertyMemoryTrackerGuard = usePropertyMemoryTrackerGuard(
  () => props.noteId
)

async function beforeSaveContent(
  lastSaved: string,
  newValue: string
): Promise<boolean> {
  if (!props.asMarkdown) {
    return true
  }
  const changes = diffFrontmatterPropertyKeyChanges(lastSaved, newValue)
  if (changes.length === 0) {
    return true
  }
  return propertyMemoryTrackerGuard.confirmAndApplyPropertyKeyChanges(changes)
}

const rootRef = ref<HTMLElement | null>(null)
const textareaRef = ref<InstanceType<typeof TextArea> | null>(null)
const richEditorRef = ref<InstanceType<typeof RichMarkdownEditor> | null>(null)
const {
  pasteChoice,
  clearPasteChoice,
  pausePasteChoiceExpiry,
  resumePasteChoiceExpiry,
  handleTextareaModelUpdate,
  handleTextareaPaste,
  handlePasteComplete,
} = useNoteContentPaste({
  noteId: () => props.noteId,
  asMarkdown: () => props.asMarkdown,
  noteContent: () => props.noteContent,
  rootRef,
  textareaRef,
})

const {
  registerInserter,
  registerInsertWikiLinkAsPropertyInserter,
  unregisterInserter,
} = useContentCursorInserter()

/** Byte offset in `markdown` of the `""` YAML key for the empty property name line, or null. */
function caretOffsetForEmptyPropertyYamlKey(markdown: string): number | null {
  if (!markdown.startsWith("---\n")) return null
  const close = markdown.indexOf("\n---\n", 4)
  if (close === -1) return null
  const yamlInner = markdown.slice(4, close)
  const m = /^""\s*:/m.exec(yamlInner)
  if (!m || m.index === undefined) return null
  return 4 + m.index
}

/** Tracks the last known textarea cursor position for markdown editor. */
const textareaSelection = ref<{ start: number; end: number } | null>(null)

function captureTextareaSelection() {
  const textarea = textareaRef.value?.$el?.querySelector(
    "textarea"
  ) as HTMLTextAreaElement | null
  if (textarea) {
    textareaSelection.value = {
      start: textarea.selectionStart,
      end: textarea.selectionEnd,
    }
  }
}

onMounted(() => {
  registerInserter((text: string) => {
    if (props.asMarkdown) {
      const textarea = textareaRef.value?.$el?.querySelector(
        "textarea"
      ) as HTMLTextAreaElement | null
      if (textarea) {
        const start = textareaSelection.value?.start ?? textarea.value.length
        const end = textareaSelection.value?.end ?? textarea.value.length
        const current = textarea.value
        const newValue = current.slice(0, start) + text + current.slice(end)
        textarea.value = newValue
        textarea.dispatchEvent(new Event("input", { bubbles: true }))
        nextTick(() => {
          textarea.selectionStart = textarea.selectionEnd = start + text.length
        })
      }
    } else {
      // Queue a microtask so we fire after the current synchronous call stack
      // but before macrotasks (requestAnimationFrame / setTimeout). This gives
      // Vue time to flush dialog-teardown DOM updates while still keeping the
      // component state intact for the insertion.
      // insertTextAtCursor uses the last known Quill cursor when available
      // (note was in edit mode); otherwise falls back to insertMarkdownAtEnd.
      queueMicrotask(() => {
        richEditorRef.value?.insertTextAtCursor(text)
      })
    }
  })

  registerInsertWikiLinkAsPropertyInserter({
    canInsert: () => parseNoteContentMarkdown(props.noteContent ?? "").ok,
    insert: (text: string) => {
      const composed = appendWikiLinkPropertyRow(props.noteContent ?? "", text)
      if (composed === undefined) return
      if (props.asMarkdown) {
        const textarea = textareaRef.value?.$el?.querySelector(
          "textarea"
        ) as HTMLTextAreaElement | null
        if (!textarea) return
        textarea.value = composed
        textarea.dispatchEvent(new Event("input", { bubbles: true }))
        nextTick(() => {
          const pos =
            caretOffsetForEmptyPropertyYamlKey(composed) ??
            (composed.startsWith("---\n") ? 4 : 0)
          textarea.selectionStart = textarea.selectionEnd = pos
        })
      } else {
        queueMicrotask(() => {
          richEditorRef.value?.addWikiLinkAsProperty(text)
        })
      }
    },
  })
})

onUnmounted(() => {
  unregisterInserter()
})
</script>
