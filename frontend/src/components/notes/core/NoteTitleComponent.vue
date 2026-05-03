<template>
  <span class="title-text">{{ title }}</span>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed } from "vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"

const props = defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
})

const title = computed(() => props.noteTopology.title ?? "")
</script>
