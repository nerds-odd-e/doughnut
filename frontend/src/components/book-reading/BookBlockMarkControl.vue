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
        v-for="(label, status) in dispositionLabels"
        :key="status"
        type="button"
        :data-testid="`book-reading-change-mark-to-${status.toLowerCase()}`"
        class="daisy-btn daisy-btn-xs daisy-btn-outline"
        @click="changeMark(status)"
      >
        {{ label }}
      </button>
      <button
        type="button"
        data-testid="book-reading-clear-mark"
        class="daisy-btn daisy-btn-xs daisy-btn-outline"
        @click="clearMark"
      >
        Clear mark
      </button>
    </template>
  </div>
</template>

<script setup lang="ts">
import {
  dispositionLabels,
  type BookBlockReadingDisposition,
} from "@/lib/book-reading/readBlockIdsFromRecords"
import { ref } from "vue"

defineProps<{ disposition: BookBlockReadingDisposition }>()

const emit = defineEmits<{
  change: [status: BookBlockReadingDisposition]
  clear: []
}>()

const menuOpen = ref(false)

function changeMark(status: BookBlockReadingDisposition) {
  menuOpen.value = false
  emit("change", status)
}

function clearMark() {
  menuOpen.value = false
  emit("clear")
}
</script>
