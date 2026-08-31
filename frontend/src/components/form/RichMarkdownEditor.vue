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
    <div
      v-if="frontmatterParseErrorMessage !== null"
      role="alert"
      aria-live="polite"
      data-testid="rich-note-frontmatter-parse-error"
      class="daisy-alert daisy-alert-warning mb-3 text-sm"
    >
      <span>{{ frontmatterParseErrorMessage }}</span>
      <span class="block mt-1 text-xs opacity-90">
        Switch to Markdown mode to fix the frontmatter.
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
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, type PropType } from "vue"
import QuillEditor from "./QuillEditor.vue"
import RichFrontmatterProperties from "./RichFrontmatterProperties.vue"
import markdownizer from "./markdownizer"
import type { WikiLink } from "@generated/donut-backend-api"
import { replaceWikiLinksInHtml } from "./replaceWikiLinksInHtml"
import {
  composeNoteContentFromPropertyRows,
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
  (e: "pasteComplete", value: string): void
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

const frontmatterParseErrorMessage = computed(() => {
  const p = parsedContent.value
  return p.ok ? null : p.message
})

const effectiveReadonly = computed(
  () =>
    Boolean(props.readonly) ||
    !parsedContent.value.ok ||
    imageUploadInProgress.value
)

const markdownForRichDisplay = computed(() => {
  const p = parsedContent.value
  if (p.ok) return p.body
  return props.modelValue ?? ""
})

const htmlWithWikiLinks = (html: string) =>
  replaceWikiLinksInHtml(html, props.wikiLinks, props.lastSavedMarkdown)

const htmlValue = computed(() => {
  const p = parsedContent.value
  if (
    currentIntervalHtml !== undefined &&
    currentIntervalBodyMarkdown !== undefined &&
    p.ok &&
    p.body === currentIntervalBodyMarkdown
  ) {
    return htmlWithWikiLinks(currentIntervalHtml)
  }
  if (
    currentIntervalHtml !== undefined &&
    currentIntervalBodyMarkdown !== undefined &&
    !p.ok &&
    (props.modelValue ?? "") === currentIntervalBodyMarkdown
  ) {
    return htmlWithWikiLinks(currentIntervalHtml)
  }
  return htmlWithWikiLinks(
    markdownizer.markdownToHtml(markdownForRichDisplay.value)
  )
})

const htmlValueUpdated = (newHtmlValue: string) => {
  const p = parsedContent.value
  if (!p.ok) return

  const bodyMarkdown = markdownizer.htmlToMarkdown(newHtmlValue)
  currentIntervalBodyMarkdown = bodyMarkdown
  currentIntervalHtml = newHtmlValue

  const prevFull = props.modelValue ?? ""
  const composed = composeNoteContentFromPropertyRows(
    frontmatterPropertiesRef.value?.getPropertyRows() ?? [],
    bodyMarkdown
  )
  if (composed === prevFull) return
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

const onPasteComplete = (html: string) => {
  const bodyMarkdown = markdownizer.htmlToMarkdown(html)
  const p = parsedContent.value
  if (!p.ok) {
    emits("pasteComplete", bodyMarkdown)
    return
  }
  const composed = composeNoteContentFromPropertyRows(
    frontmatterPropertiesRef.value?.getPropertyRows() ?? [],
    bodyMarkdown
  )
  emits("pasteComplete", composed)
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

function addWikiLinkAsProperty(text: string) {
  frontmatterPropertiesRef.value?.addWikiLinkAsProperty(text)
}

defineExpose({ insertMarkdownAtEnd, insertTextAtCursor, addWikiLinkAsProperty })
</script>
