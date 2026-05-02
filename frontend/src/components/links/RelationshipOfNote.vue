<template>
  <span class="relationship-container daisy-inline-flex daisy-items-center daisy-gap-1">
    <RelationNob
      v-if="!!reverse"
      :relation-type-label="relationTypeLabel"
      :inverse-icon="true"
    />
    <router-link
      :to="noteShowLocation"
      class="relationship-title daisy-no-underline daisy-font-medium hover:daisy-underline daisy-transition-all"
    >
      <NoteTitleComponent v-if="titleTopology" v-bind="{ noteTopology: titleTopology }" />
    </router-link>
    <RelationNob
      v-if="!reverse"
      :relation-type-label="relationTypeLabel"
      :inverse-icon="false"
    />
  </span>
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import { computed } from "vue"
import { noteShowLocationFromNoteTopology } from "@/routes/noteShowLocation"
import { relationTypeLabelFromNoteDetails } from "@/models/relationTypeOptions"
import { colors } from "../../colors"
import NoteTitleComponent from "../notes/core/NoteTitleComponent.vue"
import RelationNob from "./RelationNob.vue"

const props = defineProps<{
  note: Note
  reverse?: boolean
}>()

const relationTypeLabel = computed(() =>
  relationTypeLabelFromNoteDetails(props.note.details)
)

/** Route to the relationship note (not subject/target). */
const relationshipTopology = computed(() => props.note.noteTopology)

/** Label in the link: referring note (reverse) or target (forward). */
const titleTopology = computed(() =>
  props.reverse
    ? props.note.noteTopology.parentOrSubjectNoteTopology!
    : props.note.noteTopology.targetNoteTopology!
)

const noteShowLocation = computed(() =>
  noteShowLocationFromNoteTopology(relationshipTopology.value)
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
