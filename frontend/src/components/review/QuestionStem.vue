<template>
  <div
    class="daisy:whitespace-pre-wrap"
    data-test="stem"
    v-if="stem"
    v-html="stemHtml"
  ></div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import markdownizer from "../form/markdownizer"

const props = defineProps<{
  stem: string | undefined
}>()

const stemHtml = computed(() => {
  return props.stem ? markdownizer.markdownToHtml(props.stem) : ""
})
</script>
