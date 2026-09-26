<template>
  <div ref="editor"></div>
</template>

<script setup lang="ts">
import { getCurrentInstance, nextTick, ref, onMounted, watch } from "vue"
import type { Router } from "vue-router"
import Quill, { Delta } from "quill"
import "quill/dist/quill.bubble.css"
import { registerDonutQuillBlots } from "./registerDonutQuillBlots"
import { donutQuillOptions } from "./donutQuillOptions"
import {
  handleRichContentAnchorClick,
  type DeadWikiLinkPayload,
} from "@/utils/wikiLinkMarkup"
import { DEAD_WIKI_LINK_CLASS } from "@/utils/wikiLinkDomMarkers"
import { interceptRichPaste, type QuillPasteContext } from "./quillPasteContext"
import {
  toPasteChoiceAnchorRect,
  type PasteChoiceAnchorRect,
} from "@/composables/pasteChoicePosition"

registerDonutQuillBlots()

const props = defineProps({
  modelValue: String,
  readonly: Boolean,
  placeholder: { type: String, default: "Enter note content here..." },
})

const emits = defineEmits<{
  "update:modelValue": [value: string]
  blur: []
  pasteComplete: [content: string, quillContext: QuillPasteContext | null]
  deadWikiLinkClick: [payload: DeadWikiLinkPayload]
  /** The HTML Quill holds after taking in a model value, which the next edit saves from. */
  modelLoaded: [html: string]
}>()

const router = getCurrentInstance()?.appContext.config.globalProperties
  .$router as Router | undefined
const editor = ref<HTMLElement | null>(null)
const quill = ref<Quill | null>(null)
const isPasting = ref(false)
const lastRange = ref<{ index: number; length: number } | null>(null)
let pendingPaste: Omit<QuillPasteContext, "insertedLength"> | null = null
let syncingModel = false

const modelHtml = () => props.modelValue || "<p><br></p>"

const onBlurTextField = () => {
  emits("blur")
}

const syncQuillFromModel = () => {
  if (!quill.value) return
  const html = modelHtml()
  if (quill.value.root.innerHTML === html) return
  syncingModel = true
  quill.value.root.innerHTML = html
  queueMicrotask(() => {
    syncingModel = false
    emits("modelLoaded", quill.value!.root.innerHTML)
  })
}

onMounted(async () => {
  if (editor.value) {
    quill.value = new Quill(
      editor.value,
      donutQuillOptions(props.readonly, props.placeholder)
    )

    syncQuillFromModel()

    await nextTick()

    if (!props.readonly && quill.value) {
      quill.value.root.addEventListener(
        "paste",
        (event: ClipboardEvent) => {
          if (!event.clipboardData) return
          pendingPaste = interceptRichPaste(quill.value!, event.clipboardData)

          // Mark paste in progress; emit after Quill updates content
          isPasting.value = true
        },
        true
      )
    }

    quill.value.root.addEventListener(
      "mousedown",
      (event: MouseEvent) => {
        if (props.readonly) return
        const anchor = (event.target as HTMLElement).closest(
          `a.${DEAD_WIKI_LINK_CLASS}`
        )
        if (anchor) event.preventDefault()
      },
      true
    )

    quill.value.root.addEventListener(
      "click",
      (event: MouseEvent) => {
        const anchor = (event.target as HTMLElement).closest("a")
        if (!anchor) return
        event.preventDefault()
        handleRichContentAnchorClick(
          anchor,
          {
            onDeadWikiLink: (payload) => emits("deadWikiLinkClick", payload),
            navigateInApp: (to) => {
              router?.push(to)
            },
          },
          { deadWikiLinksEnabled: !props.readonly }
        )
      },
      true
    )

    quill.value.on("text-change", (delta) => {
      const content = quill.value!.root.innerHTML
      if (!syncingModel && content !== modelHtml()) {
        emits("update:modelValue", content)
      }
      if (isPasting.value) {
        isPasting.value = false
        const context: QuillPasteContext | null = pendingPaste
          ? {
              ...pendingPaste,
              insertedLength:
                delta.length() -
                pendingPaste.range.index -
                pendingPaste.range.length,
            }
          : null
        pendingPaste = null
        emits("pasteComplete", content, context)
      }
    })

    quill.value.on("selection-change", (range) => {
      if (!range) {
        onBlurTextField()
      } else {
        lastRange.value = { index: range.index, length: range.length }
      }
    })

    // Forward DOM blur to Quill only when focus leaves the editor (not to a link inside it).
    quill.value.root.addEventListener("blur", (event: FocusEvent) => {
      const related = event.relatedTarget
      if (related instanceof Node && quill.value?.root.contains(related)) {
        return
      }
      quill.value?.blur()
    })
  }
})

watch(() => props.modelValue, syncQuillFromModel)

/** Places the caret at `index` without emitting a selection-change event.
 * Swallows a "DOM not ready" failure: both callers have already applied
 * their content change, so a caret placement issue must not undo that. */
function setSelectionSilently(index: number) {
  try {
    quill.value?.setSelection(index, 0, Quill.sources.SILENT)
  } catch {
    // ignore if editor DOM is not ready
  }
}

function insertTextAtCursor(text: string) {
  if (!quill.value) return
  if (lastRange.value === null) {
    // Editor had no cursor (e.g. note was in readonly/view mode).
    // Fall through to the caller's insertMarkdownAtEnd path.
    return false
  }
  const index = lastRange.value.index
  // Tell the caller that we handled it
  quill.value.insertText(index, text, Quill.sources.USER)
  setSelectionSilently(index + text.length)
  return true
}

/** Swaps the span a rich paste inserted (`context.range.index` for
 * `context.insertedLength` characters) back to `text`, via a single Delta
 * retain/delete/insert so undo history and the `text-change` listener above
 * (which emits `update:modelValue`) both see one ordinary user edit. */
function replacePastedRange(context: QuillPasteContext, text: string) {
  if (!quill.value) return
  quill.value.updateContents(
    new Delta()
      .retain(context.range.index)
      .delete(context.insertedLength)
      .insert(text),
    Quill.sources.USER
  )
  setSelectionSilently(context.range.index + text.length)
}

/** Viewport geometry of a rich paste's inserted span, for placing the paste-choice
 * action bar clear of it. Quill's `getBounds()` already returns viewport-relative
 * coordinates (it delegates to the native DOM `Range`/`Element` `getBoundingClientRect()`). */
function pasteInsertionViewportRect(range: {
  index: number
  length: number
}): PasteChoiceAnchorRect | null {
  const bounds = quill.value?.getBounds(range.index, range.length)
  return bounds ? toPasteChoiceAnchorRect(bounds) : null
}

defineExpose({
  insertTextAtCursor,
  replacePastedRange,
  pasteInsertionViewportRect,
})
</script>
