<template>
  <span class="link-container daisy-inline-flex daisy-items-center daisy-gap-1">
    <LinkNob v-bind="{ noteTopology }" v-if="!!reverse" :inverse-icon="true" />
    <router-link
      :to="{ name: 'noteShow', params: { noteId: note.id } }"
      class="link-title daisy-no-underline daisy-font-medium hover:daisy-underline daisy-transition-all"
    >
      <NoteTitleComponent v-if="noteTopology" v-bind="{ noteTopology }" />
    </router-link>
    <LinkNob v-bind="{ noteTopology }" v-if="!reverse" :inverse-icon="false" />
  </span>
</template>

<script setup lang="ts">
import type { Note } from "@generated/backend"
import { computed } from "vue"
import { colors } from "../../colors"
import NoteTitleComponent from "../notes/core/NoteTitleComponent.vue"
import LinkNob from "./LinkNob.vue"

const props = defineProps<{
  note: Note
  reverse?: boolean
}>()

const noteTopology = computed(() => {
  return props.reverse
    ? props.note.noteTopology.parentOrSubjectNoteTopology!
    : props.note.noteTopology.objectNoteTopology!
})

const fontColor = computed(() => {
  return props.reverse ? colors.target : colors.source
})
</script>

<style scoped>
.link-title {
  color: v-bind(fontColor);
}
</style>
