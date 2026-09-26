<template>
  <div>
    <RichFrontmatterProperties
      ref="frontmatterPropertiesRef"
      :content-markdown="modelValue ?? ''"
      :read-only="readonly"
      :wiki-links="wikiLinks"
      :last-saved-markdown="lastSavedMarkdown"
      :note-title-for-wikidata-search="noteTitleForWikidataSearch"
      :note-id="noteId"
      :interaction-locked="imageUploadInProgress"
      :is-readme-context="isReadmeContext"
      @properties-changed="onPropertiesChanged"
      @dead-wiki-link-click="$emit('deadWikiLinkClick', $event)"
      @image-upload-state="imageUploadInProgress = $event"
    />
    <p v-if="hasNestedMetadata" class="mb-3 text-sm">
      Edit metadata in Markdown.
    </p>
    <div
      v-if="richEditingUnavailableReason"
      role="alert"
      aria-live="polite"
      data-testid="rich-note-unavailable-warning"
      class="daisy-alert daisy-alert-warning mb-3 text-sm"
    >
      <span>{{ richEditingUnavailableReason.message }}</span>
      <span class="block mt-1 text-xs opacity-90">
        {{ richEditingUnavailableReason.hint }}
      </span>
    </div>
    <QuillEditor
      ref="quillRef"
      v-bind="{ multipleLine, scopeName, field, title, errors }"
      :model-value="htmlValue"
      :readonly="effectiveReadonly"
      :placeholder="
        isReadmeContext ? 'Enter readme content here...' : undefined
      "
      @update:model-value="htmlValueUpdated"
      @blur="$emit('blur')"
      @paste-complete="onPasteComplete"
      @dead-wiki-link-click="$emit('deadWikiLinkClick', $event)"
      @model-loaded="checkBodyItCannotKeep"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, type PropType } from "vue"
import QuillEditor from "./QuillEditor.vue"
import type { QuillPasteContext } from "./quillPasteContext"
import RichFrontmatterProperties from "./RichFrontmatterProperties.vue"
import markdownizer from "./markdownizer"
import { richEditorKeepsBody } from "./richEditorKeepsBody"
import type { WikiLink } from "@generated/donut-backend-api"
import { replaceWikiLinksInHtml } from "./replaceWikiLinksInHtml"
import {
  composeNoteContentFromPropertyRows,
  composeNoteContentInPlace,
  parseNoteContentMarkdown,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"

const quillRef = ref<InstanceType<typeof QuillEditor> | null>(null)

const props = defineProps({
  multipleLine: Boolean,
  modelValue: String,
  scopeName: String,
  field: String,
  title: String,
  errors: Object,
  readonly: Boolean,
  wikiLinks: { type: Array as PropType<WikiLink[]>, required: true },
  lastSavedMarkdown: { type: String, default: undefined },
  noteTitleForWikidataSearch: { type: String, default: "" },
  noteId: { type: Number as PropType<number | undefined>, default: undefined },
  isReadmeContext: { type: Boolean, default: false },
})

const emits = defineEmits<{
  (e: "update:modelValue", value: string): void
  (e: "blur"): void
  (
    e: "pasteComplete",
    value: string,
    quillContext: QuillPasteContext | null
  ): void
  (e: "deadWikiLinkClick", payload: DeadWikiLinkPayload): void
}>()

/** Body markdown (or full note content when frontmatter could not be parsed). */
let currentIntervalBodyMarkdown: string | undefined
let currentIntervalHtml: string | undefined

const frontmatterPropertiesRef = ref<InstanceType<
  typeof RichFrontmatterProperties
> | null>(null)

const imageUploadInProgress = ref(false)

const parsedContent = computed(() =>
  parseNoteContentMarkdown(props.modelValue ?? "")
)

const bodyItCannotKeep = ref(false)

const richEditingUnavailableReason = computed(() => {
  const p = parsedContent.value
  if (!p.ok && p.reason !== "nested_metadata")
    return {
      message: p.message,
      hint: "Switch to Markdown mode to fix the frontmatter.",
    }
  if (bodyItCannotKeep.value)
    return {
      message: "This note has content the rich editor cannot keep.",
      hint: "Switch to Markdown mode to edit it.",
    }
  return null
})

const hasNestedMetadata = computed(() => {
  const p = parsedContent.value
  return !p.ok && p.reason === "nested_metadata"
})

const effectiveReadonly = computed(
  () =>
    Boolean(props.readonly) ||
    richEditingUnavailableReason.value !== null ||
    imageUploadInProgress.value
)

const markdownForRichDisplay = computed(() => {
  const p = parsedContent.value
  if (p.ok || p.reason === "nested_metadata") return p.body
  return props.modelValue ?? ""
})

const checkBodyItCannotKeep = (heldHtml: string) => {
  const body = markdownForRichDisplay.value
  if (props.readonly || body === currentIntervalBodyMarkdown) return
  bodyItCannotKeep.value = !richEditorKeepsBody(body, heldHtml)
}

const htmlWithWikiLinks = (html: string) =>
  replaceWikiLinksInHtml(html, props.wikiLinks, props.lastSavedMarkdown)

const htmlValue = computed(() => {
  if (
    currentIntervalHtml !== undefined &&
    currentIntervalBodyMarkdown === markdownForRichDisplay.value
  ) {
    return htmlWithWikiLinks(currentIntervalHtml)
  }
  return htmlWithWikiLinks(
    markdownizer.markdownToHtml(markdownForRichDisplay.value)
  )
})

const composeBodyMarkdown = (bodyMarkdown: string) =>
  composeNoteContentInPlace(
    props.modelValue ?? "",
    frontmatterPropertiesRef.value?.getPropertyRows() ?? [],
    bodyMarkdown
  )

const htmlValueUpdated = (newHtmlValue: string) => {
  if (effectiveReadonly.value) return

  const bodyMarkdown = markdownizer.htmlToMarkdown(newHtmlValue)
  currentIntervalBodyMarkdown = bodyMarkdown
  currentIntervalHtml = newHtmlValue

  const composed = composeBodyMarkdown(bodyMarkdown)
  if (composed === (props.modelValue ?? "")) return
  emits("update:modelValue", composed)
}

const onPropertiesChanged = (rows: PropertyRow[]) => {
  const p = parsedContent.value
  if (!p.ok) return
  const prevFull = props.modelValue ?? ""
  const bodyMarkdown =
    currentIntervalBodyMarkdown !== undefined
      ? currentIntervalBodyMarkdown
      : p.body
  const composed = composeNoteContentFromPropertyRows(rows, bodyMarkdown)
  if (composed === prevFull) return
  emits("update:modelValue", composed)
  nextTick(() => {
    emits("blur")
  })
}

const onPasteComplete = (
  html: string,
  quillContext: QuillPasteContext | null
) => {
  if (effectiveReadonly.value) return
  emits(
    "pasteComplete",
    composeBodyMarkdown(markdownizer.htmlToMarkdown(html)),
    quillContext
  )
}

function insertMarkdownAtEnd(text: string) {
  const current = props.modelValue ?? ""
  emits("update:modelValue", current + text)
}

function insertTextAtCursor(text: string) {
  if (!quillRef.value?.insertTextAtCursor(text)) {
    insertMarkdownAtEnd(text)
  }
}

function replacePastedRange(context: QuillPasteContext, text: string) {
  quillRef.value?.replacePastedRange(context, text)
}

function pasteInsertionViewportRect(range: { index: number; length: number }) {
  return quillRef.value?.pasteInsertionViewportRect(range) ?? null
}

function addWikiLinkAsProperty(text: string) {
  frontmatterPropertiesRef.value?.addWikiLinkAsProperty(text)
}

defineExpose({
  insertMarkdownAtEnd,
  insertTextAtCursor,
  addWikiLinkAsProperty,
  replacePastedRange,
  pasteInsertionViewportRect,
})
</script>
