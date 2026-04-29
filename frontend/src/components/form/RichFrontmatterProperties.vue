<template>
  <section
    v-if="showSection"
    class="daisy-mb-3"
    :aria-labelledby="headingId"
  >
    <h4
      :id="headingId"
      class="daisy-text-sm daisy-font-semibold daisy-mb-2"
    >
      Properties
    </h4>
    <dl
      class="daisy-grid daisy-grid-cols-[auto_minmax(0,1fr)] daisy-gap-x-4 daisy-gap-y-1 daisy-text-sm"
    >
      <template v-for="row in propertyRows" :key="row.key">
        <dt class="daisy-font-medium daisy-text-base-content/80">{{ row.key }}</dt>
        <dd class="daisy-m-0">{{ row.value }}</dd>
      </template>
    </dl>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, useId, watch } from "vue"
import {
  parseNoteDetailsMarkdown,
  sortedPropertyRowsFromRecord,
  type PropertyRow,
} from "@/utils/noteDetailsFrontmatter"

const props = defineProps<{
  detailsMarkdown: string
}>()

const headingId = useId()

const propertyRows = ref<PropertyRow[]>([])

watch(
  () => props.detailsMarkdown,
  () => {
    const p = parseNoteDetailsMarkdown(props.detailsMarkdown)
    if (p.ok) {
      propertyRows.value = sortedPropertyRowsFromRecord(p.properties)
    } else {
      propertyRows.value = []
    }
  },
  { immediate: true }
)

const showSection = computed(() => propertyRows.value.length > 0)

defineExpose({
  getPropertyRows: (): PropertyRow[] => propertyRows.value,
})
</script>
