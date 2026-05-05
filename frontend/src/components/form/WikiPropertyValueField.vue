<template>
  <div
    ref="root"
    tabindex="0"
    class="wiki-property-value-field daisy-input daisy-input-bordered daisy-input-sm daisy-w-full daisy-min-h-0 daisy-py-1 daisy-px-3 daisy-leading-normal"
    :class="{ 'daisy-input-disabled': readonly }"
    :contenteditable="!readonly"
    role="textbox"
    :aria-label="ariaLabel"
    :data-testid="dataTestid"
    @input="onInput"
    @blur="emit('blur')"
    @click.capture="onClickCapture"
    @keydown.enter.prevent="onEnter"
    @paste="onPaste"
  />
</template>

<script setup lang="ts">
import { ref, watch, onMounted, type PropType } from "vue"
import { useRouter } from "vue-router"
import type { WikiTitle } from "@generated/doughnut-backend-api"
import {
  propertyValuePlainToDisplayHtml,
  serializeWikiPropertyValueFieldRoot,
} from "@/utils/wikiPropertyValueField"

const props = defineProps({
  modelValue: { type: String, required: true },
  wikiTitles: {
    type: Array as PropType<WikiTitle[]>,
    required: true,
  },
  readonly: { type: Boolean, default: false },
  ariaLabel: { type: String, required: false },
  dataTestid: { type: String, required: false },
})

const emit = defineEmits<{
  "update:modelValue": [value: string]
  blur: []
  deadLinkClick: [title: string]
}>()

const router = useRouter()
const root = ref<HTMLElement | null>(null)

/** Matches `props.modelValue` after our own emit; skips DOM replace when parent echoes the same string. */
const lastEmittedPlain = ref("")

function pushDisplayHtml(plain: string) {
  if (!root.value) return
  root.value.innerHTML = propertyValuePlainToDisplayHtml(
    plain,
    props.wikiTitles
  )
}

watch(
  () => props.modelValue,
  (v) => {
    const plain = v ?? ""
    if (plain === lastEmittedPlain.value) return
    lastEmittedPlain.value = plain
    pushDisplayHtml(plain)
  }
)

watch(
  () => props.wikiTitles,
  () => {
    const plain = props.modelValue ?? ""
    lastEmittedPlain.value = plain
    pushDisplayHtml(plain)
  },
  { deep: true }
)

onMounted(() => {
  lastEmittedPlain.value = props.modelValue ?? ""
  pushDisplayHtml(lastEmittedPlain.value)
})

function onInput() {
  if (props.readonly || !root.value) return
  const plain = serializeWikiPropertyValueFieldRoot(root.value)
  lastEmittedPlain.value = plain
  emit("update:modelValue", plain)
}

function onClickCapture(event: MouseEvent) {
  if (props.readonly || !root.value) return
  const anchor = (event.target as HTMLElement).closest("a")
  if (!anchor || !root.value.contains(anchor)) return
  event.preventDefault()
  const href = anchor.getAttribute("href")
  if (!href) return
  if (!props.readonly && anchor.classList.contains("dead-link")) {
    const title =
      anchor.getAttribute("data-wiki-title")?.trim() ??
      anchor.textContent
        ?.replace(/^\s*\[\[\s*/, "")
        .replace(/\s*\]\]\s*$/, "")
        .trim() ??
      ""
    emit("deadLinkClick", title)
    return
  }
  if (/^https?:\/\//i.test(href) || href.startsWith("//")) {
    window.open(href, "_blank", "noopener,noreferrer")
    return
  }
  router.push(href)
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
.wiki-property-value-field {
  outline: none;
  white-space: nowrap;
  overflow-x: auto;
  overflow-y: hidden;
  line-height: 1.375rem;
}

.wiki-property-value-field :deep(a) {
  cursor: pointer;
}

.wiki-property-value-field :deep(a.doughnut-link) {
  color: oklch(var(--a));
  text-decoration: underline;
  text-decoration-style: dotted;
  text-underline-offset: 0.15em;
}

.wiki-property-value-field :deep(a:not(.doughnut-link):not(.dead-link)) {
  color: oklch(var(--in));
  text-decoration: underline;
  text-underline-offset: 0.15em;
}

.wiki-property-value-field :deep(a.dead-link) {
  color: red;
}

.wiki-property-value-field :deep(.wiki-bracket) {
  text-decoration: none;
}
</style>
