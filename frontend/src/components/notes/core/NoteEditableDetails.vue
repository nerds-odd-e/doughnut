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
        @paste="(event) => handlePaste(event, value, update)"
      />
      <RichMarkdownEditor
        v-else
        :multiple-line="true"
        scope-name="note"
        :model-value="value"
        :readonly="readonly"
        @update:model-value="update(noteId, $event)"
        @blur="blur"
      />
    </template>
  </TextContentWrapper>
</template>

<script setup lang="ts">
import { nextTick, ref } from "vue"
import RichMarkdownEditor from "../../form/RichMarkdownEditor.vue"
import TextContentWrapper from "./TextContentWrapper.vue"
import TextArea from "@/components/form/TextArea.vue"
import { useInterruptingHtmlToMarkdown } from "@/composables/useInterruptingHtmlToMarkdown"

const props = defineProps({
  noteId: { type: Number, required: true },
  noteDetails: { type: String, required: false },
  readonly: { type: Boolean, default: true },
  asMarkdown: Boolean,
})

const textareaRef = ref<InstanceType<typeof TextArea> | null>(null)
const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()

async function handleHtmlPaste(
  event: ClipboardEvent,
  callback: (markdown: string) => void
): Promise<boolean> {
  const htmlData = event.clipboardData?.getData("text/html")
  if (htmlData) {
    event.preventDefault()
    const markdown = htmlToMarkdown(htmlData)
    callback(markdown)
    return true
  }
  return false
}

const handlePaste = async (
  event: ClipboardEvent,
  currentValue: string | undefined,
  update: (noteId: number, newValue: string) => void
) => {
  if (!props.asMarkdown || !textareaRef.value) return

  const handled = await handleHtmlPaste(event, (markdown) => {
    const textarea = textareaRef.value?.$el?.querySelector(
      "textarea"
    ) as HTMLTextAreaElement | null
    if (textarea) {
      const start = textarea.selectionStart
      const end = textarea.selectionEnd
      const newValue =
        (currentValue || "").slice(0, start) +
        markdown +
        (currentValue || "").slice(end)
      update(props.noteId, newValue)
      // Set cursor position after pasted content
      nextTick(() => {
        if (textarea) {
          textarea.selectionStart = textarea.selectionEnd =
            start + markdown.length
        }
      })
    }
  })

  // If HTML was handled, prevent default to stop TextArea's plain text handler
  // If no HTML, let TextArea's handler process plain text
  if (handled) {
    event.preventDefault()
  }
}
</script>
