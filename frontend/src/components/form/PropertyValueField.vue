<template>
  <div
    ref="root"
    tabindex="0"
    class="property-value-field rich-content-wiki-links daisy-input daisy-input-sm w-full min-h-0 py-1 px-3 leading-normal"
    :class="{ 'daisy-input-disabled': readonly }"
    :contenteditable="!readonly"
    role="textbox"
    :aria-label="ariaLabel"
    :data-testid="dataTestid"
    @input="onInput"
    @blur="onBlur"
    @click.capture="onClickCapture"
    @keydown.enter.prevent="onEnter"
    @paste="onPaste"
  />
</template>

<script setup lang="ts">
import { ref, watch, onMounted, type PropType } from "vue"
import { useRouter } from "vue-router"
import type { WikiTitle } from "@generated/donut-backend-api"
import {
  handleRichContentAnchorClick,
  type DeadWikiLinkPayload,
} from "@/utils/wikiLinkMarkup"
import {
  propertyValuePlainToDisplayHtml,
  serializePropertyValueFieldRoot,
} from "@/utils/propertyValueField"

const props = defineProps({
  modelValue: { type: String, required: true },
  wikiTitles: {
    type: Array as PropType<WikiTitle[]>,
    required: true,
  },
  lastSavedMarkdown: { type: String, default: undefined },
  readonly: { type: Boolean, default: false },
  ariaLabel: { type: String, required: false },
  dataTestid: { type: String, required: false },
})

const emit = defineEmits<{
  "update:modelValue": [value: string]
  blur: []
  deadWikiLinkClick: [payload: DeadWikiLinkPayload]
}>()

const router = useRouter()
const root = ref<HTMLElement | null>(null)

/** Matches `props.modelValue` after our own emit; skips DOM replace when parent echoes the same string. */
const lastEmittedPlain = ref("")

function pushDisplayHtml(plain: string) {
  if (!root.value) return
  root.value.innerHTML = propertyValuePlainToDisplayHtml(
    plain,
    props.wikiTitles,
    props.lastSavedMarkdown
  )
}

function restyleFromPlain(plain: string) {
  lastEmittedPlain.value = plain
  pushDisplayHtml(plain)
}

watch(
  () => props.modelValue,
  (v) => {
    const plain = v ?? ""
    if (plain === lastEmittedPlain.value) return
    restyleFromPlain(plain)
  }
)

watch(
  () => [props.wikiTitles, props.lastSavedMarkdown] as const,
  () => {
    restyleFromPlain(props.modelValue ?? "")
  },
  { deep: true }
)

onMounted(() => {
  restyleFromPlain(props.modelValue ?? "")
})

function onInput() {
  if (props.readonly || !root.value) return
  const plain = serializePropertyValueFieldRoot(root.value)
  lastEmittedPlain.value = plain
  emit("update:modelValue", plain)
}

function onClickCapture(event: MouseEvent) {
  if (props.readonly || !root.value) return
  const anchor = (event.target as HTMLElement).closest("a")
  if (!anchor || !root.value.contains(anchor)) return
  event.preventDefault()
  handleRichContentAnchorClick(
    anchor,
    {
      onDeadWikiLink: (payload) => emit("deadWikiLinkClick", payload),
      navigateInApp: (to) => router.push(to),
    },
    { deadWikiLinksEnabled: true }
  )
}

function onBlur() {
  restyleFromPlain(props.modelValue ?? "")
  emit("blur")
}

function onEnter() {
  root.value?.blur()
}

function onPaste(event: ClipboardEvent) {
  if (props.readonly || !root.value) return
  event.preventDefault()
  const plainText = event.clipboardData?.getData("text/plain") ?? ""
  if (!plainText) return
  const inserted = document.execCommand("insertText", false, plainText)
  if (!inserted) {
    const sel = window.getSelection()
    if (sel?.rangeCount) {
      const range = sel.getRangeAt(0)
      if (root.value.contains(range.commonAncestorContainer)) {
        range.deleteContents()
        range.insertNode(document.createTextNode(plainText))
        range.collapse(false)
        sel.removeAllRanges()
        sel.addRange(range)
      } else {
        root.value.appendChild(document.createTextNode(plainText))
      }
    } else {
      root.value.appendChild(document.createTextNode(plainText))
    }
  }
  onInput()
}

defineExpose({
  focus: () => root.value?.focus(),
})
</script>

<style scoped>
.property-value-field {
  outline: none;
  white-space: nowrap;
  overflow-x: auto;
  overflow-y: hidden;
  line-height: 1.375rem;
}

.property-value-field :deep(.wiki-bracket) {
  text-decoration: none;
}
</style>
