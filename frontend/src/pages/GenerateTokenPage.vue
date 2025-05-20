<script setup lang="ts">
import { ref } from "vue"

const token = ref<string | null>(null)
const loading = ref(false)
const copied = ref(false)

const generateToken = async () => {
  try {
    loading.value = true
    copied.value = false
    await new Promise((resolve) => setTimeout(resolve, 1000))
    token.value = "example-generated-token-123456"
    loading.value = false
  } catch (error) {
    console.error("Error generating token:", error)
    loading.value = false
  }
}

const copyToken = async () => {
  if (token.value) {
    await navigator.clipboard.writeText(token.value)
    copied.value = true
    setTimeout(() => (copied.value = false), 1500)
  }
}
</script>

<template>
  <div class="daisy-container daisy-mx-auto daisy-p-4">
    <h1 class="daisy-text-2xl daisy-font-bold daisy-mb-4">Generate Token</h1>
    <button
      class="daisy-btn daisy-btn-primary"
      :disabled="loading"
      @click="generateToken"
      data-testid="generate-token-btn"
    >
      <span v-if="loading" class="daisy-loading daisy-loading-spinner daisy-loading-xs daisy-mr-2"></span>
      Generate Token
    </button>
    <div v-if="token" class="daisy-flex daisy-items-center daisy-mt-6">
      <span class="daisy-mr-2 daisy-px-2 daisy-py-1 daisy-bg-base-200 daisy-rounded daisy-font-mono" data-testid="token-result">
        {{ token }}
      </span>
      <button
        class="daisy-btn daisy-btn-ghost daisy-btn-xs"
        @click="copyToken"
        :disabled="copied"
        aria-label="Copy token"
        data-testid="copy-token-btn"
      >
        <svg v-if="!copied" xmlns="http://www.w3.org/2000/svg" class="daisy-h-4 daisy-w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16h8M8 12h8m-7 8h6a2 2 0 002-2V6a2 2 0 00-2-2H8a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        <span v-else class="daisy-text-success">Copied!</span>
      </button>
    </div>
  </div>
</template>
