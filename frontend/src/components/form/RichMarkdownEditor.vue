<template>
  <div>
    <RichFrontmatterProperties
      ref="frontmatterPropertiesRef"
      :details-markdown="modelValue ?? ''"
      :read-only="readonly"
      :wiki-titles="wikiTitles"
      @properties-changed="onPropertiesChanged"
      @dead-link-click="$emit('deadLinkClick', $event)"
    />
    <div
      v-if="frontmatterParseErrorMessage !== null"
      role="alert"
      aria-live="polite"
      data-testid="rich-note-frontmatter-parse-error"
      class="daisy-alert daisy-alert-warning daisy-mb-3 daisy-text-sm"
    >
      <span>{{ frontmatterParseErrorMessage }}</span>
      <span class="daisy-block daisy-mt-1 daisy-text-xs daisy-opacity-90">
        Switch to Markdown mode to fix the frontmatter.
      </span>
    </div>
    <QuillEditor
      ref="quillRef"
      v-bind="{ multipleLine, scopeName, field, title, errors }"
      :model-value="htmlValue"
      :readonly="effectiveReadonly"
      @update:model-value="htmlValueUpdated"
      @blur="$emit('blur')"
      @paste-complete="onPasteComplete"
      @dead-link-click="$emit('deadLinkClick', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, type PropType } from "vue"
import QuillEditor from "./QuillEditor.vue"
import RichFrontmatterProperties from "./RichFrontmatterProperties.vue"
import markdownizer from "./markdownizer"
import type { WikiTitle } from "@generated/doughnut-backend-api"
import { replaceWikiLinksInHtml } from "./replaceWikiLinksInHtml"
import {
  composeNoteDetailsFromPropertyRows,
  parseNoteDetailsMarkdown,
  type PropertyRow,
} from "@/utils/noteDetailsFrontmatter"

const quillRef = ref<InstanceType<typeof QuillEditor> | null>(null)

const props = defineProps({
  multipleLine: Boolean,
  modelValue: String,
  scopeName: String,
  field: String,
  title: String,
  errors: Object,
  readonly: Boolean,
  wikiTitles: { type: Array as PropType<WikiTitle[]>, required: true },
})

const emits = defineEmits<{
  (e: "update:modelValue", value: string): void
  (e: "blur"): void
  (e: "pasteComplete", value: string): void
  (e: "deadLinkClick", title: string): void
}>()

/** Body markdown (or full details when frontmatter could not be parsed). */
let currentIntervalBodyMarkdown: string | undefined
let currentIntervalHtml: string | undefined

const frontmatterPropertiesRef = ref<InstanceType<
  typeof RichFrontmatterProperties
> | null>(null)

const parsedDetails = computed(() =>
  parseNoteDetailsMarkdown(props.modelValue ?? "")
)

const frontmatterParseErrorMessage = computed(() => {
  const p = parsedDetails.value
  return p.ok ? null : p.message
})

const effectiveReadonly = computed(
  () => Boolean(props.readonly) || !parsedDetails.value.ok
)

const markdownForRichDisplay = computed(() => {
  const p = parsedDetails.value
  if (p.ok) return p.body
  return props.modelValue ?? ""
})

const htmlValue = computed(() => {
  const p = parsedDetails.value
  if (
    currentIntervalHtml !== undefined &&
    currentIntervalBodyMarkdown !== undefined &&
    p.ok &&
    p.body === currentIntervalBodyMarkdown
  ) {
    return replaceWikiLinksInHtml(currentIntervalHtml, props.wikiTitles)
  }
  if (
    currentIntervalHtml !== undefined &&
    currentIntervalBodyMarkdown !== undefined &&
    !p.ok &&
    (props.modelValue ?? "") === currentIntervalBodyMarkdown
  ) {
    return replaceWikiLinksInHtml(currentIntervalHtml, props.wikiTitles)
  }
  return replaceWikiLinksInHtml(
    markdownizer.markdownToHtml(markdownForRichDisplay.value),
    props.wikiTitles
  )
})

const htmlValueUpdated = (newHtmlValue: string) => {
  const p = parsedDetails.value
  if (!p.ok) return

  const bodyMarkdown = markdownizer.htmlToMarkdown(newHtmlValue)
  currentIntervalBodyMarkdown = bodyMarkdown
  currentIntervalHtml = newHtmlValue

  const prevFull = props.modelValue ?? ""
  const composed = composeNoteDetailsFromPropertyRows(
    frontmatterPropertiesRef.value?.getPropertyRows() ?? [],
    bodyMarkdown
  )
  if (composed === prevFull) return
  emits("update:modelValue", composed)
}

const onPropertiesChanged = (rows: PropertyRow[]) => {
  const p = parsedDetails.value
  if (!p.ok) return
  const prevFull = props.modelValue ?? ""
  const bodyMarkdown =
    currentIntervalBodyMarkdown !== undefined
      ? currentIntervalBodyMarkdown
      : p.body
  const composed = composeNoteDetailsFromPropertyRows(rows, bodyMarkdown)
  if (composed !== prevFull) emits("update:modelValue", composed)
}

const onPasteComplete = (html: string) => {
  const bodyMarkdown = markdownizer.htmlToMarkdown(html)
  const p = parsedDetails.value
  if (!p.ok) {
    emits("pasteComplete", bodyMarkdown)
    return
  }
  const composed = composeNoteDetailsFromPropertyRows(
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

function addWikiLinkProperty(text: string) {
  frontmatterPropertiesRef.value?.addWikiLinkProperty(text)
}

defineExpose({ insertMarkdownAtEnd, insertTextAtCursor, addWikiLinkProperty })
</script>
