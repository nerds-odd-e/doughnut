<template>
  <section
    data-testid="book-reading-content-stream"
    class="daisy-shrink-0 daisy-max-h-[30vh] daisy-min-h-0 daisy-overflow-y-auto daisy-border-t daisy-border-base-300 daisy-bg-base-200/80 daisy-px-2 daisy-py-2"
    :aria-label="ariaLabel"
  >
    <p
      v-if="selectedBlockTitle"
      class="daisy-text-xs daisy-font-medium daisy-text-base-content/70 daisy-mb-1 daisy-truncate"
    >
      {{ selectedBlockTitle }}
    </p>
    <ul
      v-if="contentBlocks.length > 0"
      class="daisy-m-0 daisy-list-none daisy-space-y-1 daisy-p-0"
    >
      <li
        v-for="block in contentBlocks"
        :key="block.id"
        data-testid="book-reading-content-block"
        :data-book-content-block-id="String(block.id)"
        class="daisy-text-sm daisy-text-base-content daisy-rounded daisy-bg-base-100 daisy-px-2 daisy-py-1"
      >
        {{ previewTextFromContentBlockRaw(block) }}
      </li>
    </ul>
    <p
      v-else
      class="daisy-m-0 daisy-text-sm daisy-text-base-content/50"
    >
      No imported body for this section.
    </p>
  </section>
</template>

<script setup lang="ts">
import { previewTextFromContentBlockRaw } from "@/lib/book-reading/contentBlockRawPreview"
import type { BookContentBlockFull } from "@generated/doughnut-backend-api"
import { computed } from "vue"

const props = defineProps<{
  contentBlocks: BookContentBlockFull[]
  selectedBlockTitle?: string | null
}>()

const ariaLabel = computed(() => {
  const t = props.selectedBlockTitle?.trim()
  return t
    ? `Imported content for ${t}`
    : "Imported content for selected section"
})
</script>
