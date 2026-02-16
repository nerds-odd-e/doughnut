<template>
  <span class="relationship-container daisy-inline-flex daisy-items-center daisy-gap-1">
    <RelationNob v-bind="{ noteTopology }" v-if="!!reverse" :inverse-icon="true" />
    <router-link
      :to="{ name: 'noteShow', params: { noteId: note.id } }"
      class="relationship-title daisy-no-underline daisy-font-medium hover:daisy-underline daisy-transition-all"
    >
      <NoteTitleComponent v-if="noteTopology" v-bind="{ noteTopology }" />
    </router-link>
    <RelationNob v-bind="{ noteTopology }" v-if="!reverse" :inverse-icon="false" />
  </span>
</template>

<script setup lang="ts">
import type { Note } from "@generated/backend"
import { computed } from "vue"
import { colors } from "../../colors"
import NoteTitleComponent from "../notes/core/NoteTitleComponent.vue"
import RelationNob from "./RelationNob.vue"

const props = defineProps<{
  note: Note
  reverse?: boolean
}>()

const noteTopology = computed(() =>
  props.reverse
    ? props.note.noteTopology.parentOrSubjectNoteTopology!
    : props.note.noteTopology.targetNoteTopology!
)

const fontColor = computed(() =>
  props.reverse ? colors.target : colors.source
)
</script>

<style scoped>
.relationship-title {
  color: v-bind(fontColor);
}
</style>
