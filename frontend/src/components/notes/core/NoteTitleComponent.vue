<template>
  <template v-if="noteTopology.targetNoteTopology">
    <template v-if="full">
      <NoteTitleWithLink
        v-if="noteTopology.parentOrSubjectNoteTopology"
        v-bind="{
          noteTopology: noteTopology.parentOrSubjectNoteTopology,
          iconized: iconizedTarget,
        }"
      />
      &nbsp;
    </template>
    <span class="relation-type" style="font-size: 50%">
      {{ relationType }}
    </span>
    <SvgRelationTypeIcon :relation-type="relationType" :inverse-icon="true" />
    &nbsp;
    <span>
      <NoteTitleComponent
        v-if="iconizedTarget"
        v-bind="{ noteTopology: noteTopology.targetNoteTopology }"
      />
      <NoteTitleWithLink
        class="hover-underline"
        v-bind="{
          noteTopology: noteTopology.targetNoteTopology,
          iconized: iconizedTarget,
        }"
      />
    </span>
  </template>
  <template v-else>
    <span class="title-text">{{ title }} </span>
  </template>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, ref } from "vue"
import type { NoteTopology } from "@generated/backend"
import SvgRelationTypeIcon from "@/components/svgs/SvgRelationTypeIcon.vue"
import NoteTitleWithLink from "../NoteTitleWithLink.vue"

const props = defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  full: { type: Boolean, default: false },
})

const reactiveProps = ref(props)

const relationType = computed(() =>
  reactiveProps.value.noteTopology.titleOrPredicate.substring(1)
)
const title = computed(() =>
  reactiveProps.value.noteTopology.titleOrPredicate?.replace(
    "%P",
    `[${reactiveProps.value.noteTopology.parentOrSubjectNoteTopology?.titleOrPredicate}]`
  )
)
const iconizedTarget = computed(
  () => !!reactiveProps.value.noteTopology.shortDetails
)
</script>

<style scoped>
.hover-underline:hover {
  text-decoration: underline !important;
}
</style>
