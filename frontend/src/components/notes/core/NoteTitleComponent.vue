<template>
  <template v-if="noteTopology.targetNoteTopology">
    <template v-if="full">
      <NoteTitleWithLink
        v-if="noteTopology.parentOrSubjectNoteTopology"
        v-bind="{ noteTopology: noteTopology.parentOrSubjectNoteTopology }"
      />
      &nbsp;
    </template>
    <span class="relation-type" style="font-size: 50%">
      {{ relationType }}
    </span>
    <SvgRelationTypeIcon v-if="relationType" :relation-type="relationType" :inverse-icon="true" />
    &nbsp;
    <span>
      <NoteTitleWithLink
        class="hover-underline"
        v-bind="{ noteTopology: noteTopology.targetNoteTopology }"
      />
    </span>
  </template>
  <template v-else>
    <span class="title-text">{{ title }} </span>
  </template>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed } from "vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"
import SvgRelationTypeIcon from "@/components/svgs/SvgRelationTypeIcon.vue"
import NoteTitleWithLink from "../NoteTitleWithLink.vue"

const props = defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  full: { type: Boolean, default: false },
})

const relationType = computed(() => props.noteTopology.relationType)
const title = computed(() => props.noteTopology.title ?? "")
</script>

<style scoped>
.hover-underline:hover {
  text-decoration: underline !important;
}
</style>
