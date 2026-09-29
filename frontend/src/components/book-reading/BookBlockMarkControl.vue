<template>
  <div
    data-testid="book-reading-book-block-mark-control"
    class="flex flex-wrap gap-1 px-2 py-1"
  >
    <button
      type="button"
      data-testid="book-reading-book-block-mark"
      class="daisy-btn daisy-btn-xs daisy-btn-ghost"
      :aria-expanded="menuOpen"
      @click="menuOpen = !menuOpen"
    >
      Marked as {{ disposition.toLowerCase() }}
    </button>
    <template v-if="menuOpen">
      <button
        v-for="option in markOptions"
        :key="option.status"
        type="button"
        :data-testid="`book-reading-change-mark-to-${option.status.toLowerCase()}`"
        class="daisy-btn daisy-btn-xs daisy-btn-outline"
        @click="changeMark(option.status)"
      >
        {{ option.label }}
      </button>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { BookBlockReadingDisposition } from "@/lib/book-reading/readBlockIdsFromRecords"
import { ref } from "vue"

defineProps<{ disposition: BookBlockReadingDisposition }>()

const emit = defineEmits<{ change: [status: BookBlockReadingDisposition] }>()

const markOptions: { status: BookBlockReadingDisposition; label: string }[] = [
  { status: "READ", label: "Read" },
  { status: "SKIMMED", label: "Skimmed" },
  { status: "SKIPPED", label: "Skipped" },
]
const menuOpen = ref(false)

function changeMark(status: BookBlockReadingDisposition) {
  menuOpen.value = false
  emit("change", status)
}
</script>
