<template>
  <div v-if="stem" data-test="stem" class="question-stem-readonly">
    <QuillEditor :model-value="stemHtml" readonly />
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import QuillEditor from "../form/QuillEditor.vue"
import markdownizer from "../form/markdownizer"

/** Markdown question stem (doughnut flavor: supports <mark> for cloze masks). Shown via read-only Quill for parity with note editor. */
const props = defineProps<{
  stem: string | undefined
}>()

const stemHtml = computed(() =>
  props.stem ? markdownizer.markdownToHtml(props.stem) : ""
)
</script>
