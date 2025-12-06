<template>
  <div ref="editor"></div>
</template>

<script setup lang="ts">
import { nextTick, ref, onMounted, watch } from "vue"
import Quill, { type QuillOptions, type Range } from "quill"
import "quill/dist/quill.bubble.css"
import markdownizer from "./markdownizer"
import { useInterruptingHtmlToMarkdown } from "@/composables/useInterruptingHtmlToMarkdown"

// Define soft line break blot
// Quill.import returns dynamic types that aren't fully typed in the Quill library
interface EmbedBlotInstance {
  // Blot instance interface
}
type EmbedBlotConstructor = new (
  node: Node,
  value?: unknown
) => EmbedBlotInstance

interface DeltaInstance {
  insert(content: unknown): DeltaInstance
}
type DeltaConstructor = new (ops?: unknown) => DeltaInstance

const Embed = Quill.import("blots/embed") as unknown as EmbedBlotConstructor
const Delta = Quill.import("delta") as unknown as DeltaConstructor

class SoftLineBreakBlot extends Embed {
  static blotName = "softbreak"
  static tagName = "br"
  static className = "softbreak"
}

class HorizontalRuleBlot extends Embed {
  static blotName = "horizontalrule"
  static tagName = "hr"
}

class TableBlot extends Embed {
  static blotName = "table"
  static tagName = "table"

  static create(value: string | { html: string }) {
    // Quill.import returns dynamic types - use type assertion for static methods
    // @ts-expect-error - Quill's Embed class has static create method but types don't reflect it
    const node = super.create() as HTMLElement
    const html = typeof value === "string" ? value : value.html
    node.innerHTML = html
    node.setAttribute("contenteditable", "false")
    return node
  }

  static value(node: HTMLElement) {
    return { html: node.innerHTML }
  }
}

// Quill.register accepts dynamic blot classes - the type system can't fully validate this
Quill.register(
  SoftLineBreakBlot as unknown as Parameters<typeof Quill.register>[0],
  true
)
Quill.register(
  HorizontalRuleBlot as unknown as Parameters<typeof Quill.register>[0],
  true
)
Quill.register(
  TableBlot as unknown as Parameters<typeof Quill.register>[0],
  true
)

const { modelValue, readonly } = defineProps({
  modelValue: String,
  readonly: Boolean,
})

const emits = defineEmits(["update:modelValue", "blur"])

const localValue = ref(modelValue)
const editor = ref<HTMLElement | null>(null)
const quill = ref<Quill | null>(null)
const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()

const onBlurTextField = () => {
  emits("blur")
}

const updateQuillContent = (content: string | undefined) => {
  if (quill.value) {
    quill.value.root.innerHTML = content ?? ""
  }
}

// Shift+Enter handler for soft line breaks
const shiftEnterHandler = function (
  this: { quill: Quill },
  range: Range | null
) {
  if (!range) return
  this.quill.insertEmbed(range.index, "softbreak", true, Quill.sources.USER)
  this.quill.insertText(range.index + 1, "\u200B", Quill.sources.USER)
  this.quill.setSelection(range.index + 1, Quill.sources.SILENT)
}

// BR matcher for clipboard operations
const brMatcher = () => {
  return new Delta().insert({ softbreak: true })
}

const options: QuillOptions = {
  modules: {
    toolbar: [
      ["bold", "italic", "underline"],
      [{ header: 1 }, { header: 2 }],
      ["blockquote", "code-block"],
      [{ list: "ordered" }, { list: "bullet" }],
      ["link"],
    ],
    keyboard: {
      bindings: {
        shiftEnter: {
          key: "Enter",
          shiftKey: true,
          handler: shiftEnterHandler,
        },
      },
    },
    clipboard: {
      matchers: [["BR", brMatcher]],
      matchVisual: false,
    },
  },
  formats: [
    "bold",
    "italic",
    "underline",
    "header",
    "blockquote",
    "code-block",
    "list",
    "link",
    "softbreak",
    "horizontalrule",
    "table",
  ],
  placeholder: readonly ? "" : "Enter note details here...",
  readOnly: readonly,
  theme: "bubble",
}

onMounted(async () => {
  if (editor.value) {
    quill.value = new Quill(editor.value, options)

    // Set initial content
    updateQuillContent(localValue.value)

    // Wait for next tick to ensure Quill is fully initialized
    await nextTick()

    if (!readonly && quill.value) {
      quill.value.root.addEventListener(
        "paste",
        (event: ClipboardEvent) => {
          if (!event.clipboardData) return

          const originalGetData = event.clipboardData.getData.bind(
            event.clipboardData
          )

          event.clipboardData.getData = (format: string) => {
            if (format === "text/html") {
              const htmlData = originalGetData(format)
              if (htmlData) {
                const markdown = htmlToMarkdown(htmlData)
                return markdownizer.markdownToHtml(markdown, {
                  preserve_pre: true,
                })
              }
            }
            return originalGetData(format)
          }
        },
        true
      )
    }

    // Listen for text changes
    quill.value.on("text-change", () => {
      const content = quill.value!.root.innerHTML
      localValue.value = content
      onUpdateContent()
    })

    quill.value.on("selection-change", (range) => {
      if (!range) {
        onBlurTextField()
      }
    })

    // Strangely, Quill does not emit a blur event when the inner editor receives a blur event
    quill.value.root.addEventListener("blur", () => {
      quill.value?.blur()
    })
  }
})

// Watch for changes in modelValue prop
watch(
  () => modelValue,
  (newValue) => {
    if (quill.value && localValue.value !== newValue) {
      localValue.value = newValue
      updateQuillContent(newValue)
    }
  }
)

const onUpdateContent = () => {
  emits("update:modelValue", localValue.value)
}
</script>

<style lang="sass">
.ql-editor
  padding: 0
  margin-bottom: 15px
  &::before
    left: 0 !important
    right: 0 !important
  p
    margin: inherit !important
  pre
    margin: inherit !important
    white-space: pre-wrap
    word-wrap: break-word
.ql-container.ql-bubble
  border: none
  font-size: inherit !important
</style>
