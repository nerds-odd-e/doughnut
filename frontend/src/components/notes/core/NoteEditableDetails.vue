<template>
  <TextContentWrapper
    :value="noteDetails"
    field="edit details"
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
        @update:model-value="update(noteId, $event)"
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
        :wiki-titles="wikiTitles"
        @update:model-value="update(noteId, $event)"
        @blur="blur"
        @paste-complete="(content) => handlePasteComplete(content, update)"
        @dead-link-click="emit('deadLinkClick', $event)"
      />
    </template>
  </TextContentWrapper>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, type PropType } from "vue"
import RichMarkdownEditor from "../../form/RichMarkdownEditor.vue"
import TextContentWrapper from "./TextContentWrapper.vue"
import TextArea from "@/components/form/TextArea.vue"
import type { WikiTitle } from "@generated/doughnut-backend-api"
import { usePasteWithLinkImageOptions } from "@/composables/usePasteWithLinkImageOptions"
import { useDetailsCursorInserter } from "@/composables/useDetailsCursorInserter"
import {
  composeNoteDetailsFromPropertyRows,
  parseNoteDetailsMarkdown,
  sortedPropertyRowsFromRecord,
  validatePropertyRowsForRichEdit,
} from "@/utils/noteDetailsFrontmatter"

const emit = defineEmits<{
  deadLinkClick: [title: string]
}>()

const props = defineProps({
  noteId: { type: Number, required: true },
  noteDetails: { type: String, required: false },
  readonly: { type: Boolean, default: true },
  asMarkdown: Boolean,
  wikiTitles: { type: Array as PropType<WikiTitle[]>, required: true },
})

const textareaRef = ref<InstanceType<typeof TextArea> | null>(null)
const richEditorRef = ref<InstanceType<typeof RichMarkdownEditor> | null>(null)
const { htmlToMarkdown, processContentAfterPaste } =
  usePasteWithLinkImageOptions()
const { registerInserter, registerWikiPropertyInserter, unregisterInserter } =
  useDetailsCursorInserter()

/** Byte offset in `details` of the `""` YAML key for the empty property name line, or null. */
function caretOffsetForEmptyPropertyYamlKey(details: string): number | null {
  if (!details.startsWith("---\n")) return null
  const close = details.indexOf("\n---\n", 4)
  if (close === -1) return null
  const yamlInner = details.slice(4, close)
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

  registerWikiPropertyInserter({
    canInsert: () => parseNoteDetailsMarkdown(props.noteDetails ?? "").ok,
    insert: (text: string) => {
      const parsed = parseNoteDetailsMarkdown(props.noteDetails ?? "")
      if (!parsed.ok) return
      const rows = [
        ...sortedPropertyRowsFromRecord(parsed.properties),
        { key: "", value: text },
      ]
      if (!validatePropertyRowsForRichEdit(rows).ok) return
      const composed = composeNoteDetailsFromPropertyRows(rows, parsed.body)
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
          richEditorRef.value?.addWikiLinkProperty(text)
        })
      }
    },
  })
})

onUnmounted(() => {
  unregisterInserter()
})

const offerToRemoveLinksAndImages = async (
  content: string,
  update: (noteId: number, newValue: string) => void
) => {
  const processedContent = await processContentAfterPaste(content)
  if (processedContent !== null) {
    update(props.noteId, processedContent)
  }
}

const handleTextareaPaste = async (
  event: ClipboardEvent,
  currentValue: string | undefined,
  update: (noteId: number, newValue: string) => void
) => {
  if (!props.asMarkdown || !textareaRef.value) return

  const htmlData = event.clipboardData?.getData("text/html")
  if (!htmlData) return

  event.preventDefault()

  const textarea = textareaRef.value?.$el?.querySelector(
    "textarea"
  ) as HTMLTextAreaElement | null
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const markdown = htmlToMarkdown(htmlData)
  const newValue =
    (currentValue || "").slice(0, start) +
    markdown +
    (currentValue || "").slice(end)

  update(props.noteId, newValue)
  nextTick(() => {
    if (textarea) {
      textarea.selectionStart = textarea.selectionEnd = start + markdown.length
    }
  })

  await offerToRemoveLinksAndImages(newValue, update)
}

const handlePasteComplete = async (
  currentValue: string | undefined,
  update: (noteId: number, newValue: string) => void
) => {
  if (!currentValue) return
  await offerToRemoveLinksAndImages(currentValue, update)
}
</script>
