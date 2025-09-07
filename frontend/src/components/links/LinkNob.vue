<template>
  <span class="link-nob" :title="noteTopology.linkType">
    <SvgLinkTypeIcon
      v-if="noteTopology.linkType"
      :link-type="noteTopology.linkType"
      :inverse-icon="inverseIcon"
    />
  </span>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { NoteTopology } from "@generated/backend"
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue"

defineProps({
  noteTopology: {
    type: Object as PropType<NoteTopology>,
    required: true,
  },
  inverseIcon: Boolean,
})
</script>
