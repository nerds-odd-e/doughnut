<template>
  <div>
    <RichFrontmatterProperties
      ref="frontmatterPropertiesRef"
      :details-markdown="modelValue ?? ''"
    />
    <QuillEditor
      v-bind="{ multipleLine, scopeName, field, title, errors }"
      :model-value="htmlValue"
      :readonly="readonly"
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
import {
  replaceWikiLinksInHtml,
  type WikiTitle,
} from "./replaceWikiLinksInHtml"
import {
  composeNoteDetailsFromPropertyRows,
  parseNoteDetailsMarkdown,
} from "@/utils/noteDetailsFrontmatter"

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
  const bodyMarkdown = markdownizer.htmlToMarkdown(newHtmlValue)
  currentIntervalBodyMarkdown = bodyMarkdown
  currentIntervalHtml = newHtmlValue

  const p = parsedDetails.value
  const prevFull = props.modelValue ?? ""

  if (p.ok) {
    const composed = composeNoteDetailsFromPropertyRows(
      frontmatterPropertiesRef.value?.getPropertyRows() ?? [],
      bodyMarkdown
    )
    if (composed === prevFull) return
    emits("update:modelValue", composed)
    return
  }

  if (bodyMarkdown === prevFull) return
  emits("update:modelValue", bodyMarkdown)
}

const onPasteComplete = (html: string) => {
  const markdown = markdownizer.htmlToMarkdown(html)
  emits("pasteComplete", markdown)
}
</script>
