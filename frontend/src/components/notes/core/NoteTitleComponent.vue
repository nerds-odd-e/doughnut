<template>
  <template v-if="noteTopology.objectNoteTopology">
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
    <span class="link-type" style="font-size: 50%">
      {{ linkType }}
    </span>
    <SvgLinkTypeIcon :link-type="linkType" :inverse-icon="true" />
    &nbsp;
    <span>
      <NoteTitleComponent
        v-if="iconizedTarget"
        v-bind="{ noteTopology: noteTopology.objectNoteTopology, clickable: false }"
      />
      <NoteTitleWithLink
        class="hover-underline"
        v-bind="{
          noteTopology: noteTopology.objectNoteTopology,
          iconized: iconizedTarget,
        }"
      />
    </span>
  </template>
  <template v-else>
    <RouterLink
      v-if="clickable"
      :to="{ name: 'noteShow', params: { noteId: noteTopology.id } }"
      class="daisy-text-decoration-none hover-underline"
    >
      <span class="title-text">{{ title }}</span>
    </RouterLink>
    <span v-else class="title-text">{{ title }}</span>
  </template>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, ref } from "vue"
import type { NoteTopology } from "@generated/backend"
import SvgLinkTypeIcon from "@/components/svgs/SvgLinkTypeIcon.vue"
import NoteTitleWithLink from "../NoteTitleWithLink.vue"
import { RouterLink } from "vue-router"

const props = defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  full: { type: Boolean, default: false },
  clickable: { type: Boolean, default: true },
})

const reactiveProps = ref(props)

const linkType = computed(() =>
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
