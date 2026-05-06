<template>
  <div>
    <textarea
      class="daisy-textarea daisy-textarea-bordered daisy-w-full daisy-h-48 daisy-bg-base-100 daisy-font-mono daisy-text-xs"
      readonly
      :value="jsonData"
      :data-testid="textareaTestId"
    />
    <div class="daisy-flex daisy-gap-2 daisy-justify-end daisy-mt-2">
      <CopyButton
        :text="jsonData"
        :disabled="!jsonData || loading"
        :test-id="copyButtonTestId"
        :aria-label="copyAriaLabel"
      />
      <button
        class="daisy-btn daisy-btn-secondary daisy-btn-circle"
        @click="downloadFile"
        :disabled="!jsonData || loading"
        :data-testid="downloadButtonTestId"
        :aria-label="downloadAriaLabel"
      >
        <Download class="daisy-w-6 daisy-h-6" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { saveAs } from "file-saver"
import CopyButton from "./CopyButton.vue"
import { Download } from "lucide-vue-next"

const props = withDefaults(
  defineProps<{
    jsonData: string
    filename: string
    loading?: boolean
    textareaTestId?: string
    copyButtonTestId?: string
    downloadButtonTestId?: string
    downloadMimeType?: string
    downloadExtension?: string
    copyAriaLabel?: string
    downloadAriaLabel?: string
  }>(),
  {
    loading: false,
    downloadMimeType: "application/json",
    downloadExtension: "json",
    copyAriaLabel: "Copy JSON",
    downloadAriaLabel: "Download JSON",
  }
)

function downloadFile() {
  if (!props.jsonData) return
  const blob = new Blob([props.jsonData], { type: props.downloadMimeType })
  saveAs(blob, `${props.filename}.${props.downloadExtension}`)
}
</script>

