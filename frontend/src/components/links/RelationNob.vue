<template>
  <span class="relation-nob" :title="noteTopology.relationType">
    <SvgRelationTypeIcon
      v-if="noteTopology.relationType"
      :relation-type="noteTopology.relationType"
      :inverse-icon="inverseIcon"
    />
  </span>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/backend"
import SvgRelationTypeIcon from "../svgs/SvgRelationTypeIcon.vue"

defineProps({
  noteTopology: {
    type: Object as PropType<NoteTopology>,
    required: true,
  },
  inverseIcon: Boolean,
})
</script>
