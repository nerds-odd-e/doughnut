<template>
  <dl
    data-testid="note-timestamps"
    class="note-reading-column flex flex-wrap gap-x-4 gap-y-1 py-2 text-sm text-base-content/60"
  >
    <div class="flex flex-wrap gap-x-1">
      <dt>Created</dt>
      <dd><time :datetime="noteTopology.createdAt">{{ formatDate(noteTopology.createdAt!) }}</time></dd>
    </div>
    <div
      data-testid="note-updated-at"
      class="flex flex-wrap gap-x-1"
      :data-freshness="freshness"
      :style="{ opacity: freshness / 100 }"
    >
      <dt>Last updated</dt>
      <dd><time :datetime="noteTopology.updatedAt">{{ formatDate(noteTopology.updatedAt!) }}</time></dd>
    </div>
  </dl>
</template>

<script setup lang="ts">
import { computed, type PropType } from "vue"
import type { NoteTopology } from "@generated/donut-backend-api"

const props = defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
})

const formatDate = (date: string) =>
  new Date(date).toLocaleDateString(undefined, {
    day: "numeric",
    month: "long",
    year: "numeric",
  })

const freshness = computed(() => {
  const ageInHours = Math.max(
    0,
    (Date.now() - new Date(props.noteTopology.updatedAt!).getTime()) /
      (1000 * 60 * 60)
  )
  return Math.round(100 - Math.min(15, Math.sqrt(ageInHours)) * 3)
})
</script>
