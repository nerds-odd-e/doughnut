<template>
  <div
    class="daisy-whitespace-pre-wrap"
    data-test="stem"
    v-if="stem"
    v-html="stemHtml"
  ></div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import markdownizer from "../form/markdownizer"

/** Markdown question stem (doughnut flavor: supports <mark> for cloze masks). Converted to HTML for display. */
const props = defineProps<{
  stem: string | undefined
}>()

const stemHtml = computed(() =>
  props.stem ? markdownizer.markdownToHtml(props.stem) : ""
)
</script>
