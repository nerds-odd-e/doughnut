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
        aria-label="Copy JSON"
      />
      <button
        class="daisy-btn daisy-btn-secondary daisy-btn-circle"
        @click="downloadJson"
        :disabled="!jsonData || loading"
        :data-testid="downloadButtonTestId"
        aria-label="Download JSON"
      >
        <SvgDownload />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { saveAs } from "file-saver"
import CopyButton from "./CopyButton.vue"
import SvgDownload from "../svgs/SvgDownload.vue"

const props = withDefaults(
  defineProps<{
    jsonData: string
    filename: string
    loading?: boolean
    textareaTestId?: string
    copyButtonTestId?: string
    downloadButtonTestId?: string
  }>(),
  {
    loading: false,
  }
)

function downloadJson() {
  if (!props.jsonData) return
  const blob = new Blob([props.jsonData], { type: "application/json" })
  saveAs(blob, `${props.filename}.json`)
}
</script>

