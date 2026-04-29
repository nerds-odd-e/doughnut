<template>
  <div>
    <section
      v-if="showReadOnlyProperties"
      class="daisy-mb-3"
      :aria-labelledby="propertiesHeadingId"
    >
      <h4
        :id="propertiesHeadingId"
        class="daisy-text-sm daisy-font-semibold daisy-mb-2"
      >
        Properties
      </h4>
      <dl
        class="daisy-grid daisy-grid-cols-[auto_minmax(0,1fr)] daisy-gap-x-4 daisy-gap-y-1 daisy-text-sm"
      >
        <template v-for="row in propertyRows" :key="row.key">
          <dt class="daisy-font-medium daisy-text-base-content/80">{{ row.key }}</dt>
          <dd class="daisy-m-0">{{ row.value }}</dd>
        </template>
      </dl>
    </section>
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
import { computed, ref, useId, watch, type PropType } from "vue"
import QuillEditor from "./QuillEditor.vue"
import markdownizer from "./markdownizer"
import {
  replaceWikiLinksInHtml,
  type WikiTitle,
} from "./replaceWikiLinksInHtml"
import {
  composeNoteDetailsFromPropertyRows,
  parseNoteDetailsMarkdown,
  sortedPropertyRowsFromRecord,
  type PropertyRow,
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

const propertiesHeadingId = useId()

const parsedDetails = computed(() =>
  parseNoteDetailsMarkdown(props.modelValue ?? "")
)

const propertyRows = ref<PropertyRow[]>([])

watch(
  () => props.modelValue,
  () => {
    const p = parseNoteDetailsMarkdown(props.modelValue ?? "")
    if (p.ok) {
      propertyRows.value = sortedPropertyRowsFromRecord(p.properties)
    } else {
      propertyRows.value = []
    }
  },
  { immediate: true }
)

const showReadOnlyProperties = computed(() => propertyRows.value.length > 0)

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
      propertyRows.value,
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
