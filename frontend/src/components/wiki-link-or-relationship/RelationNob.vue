<template>
  <span class="relation-nob" :title="relationTypeLabel">
    <SvgRelationTypeIcon
      v-if="relationTypeLabel"
      :relation-type="relationTypeLabel"
      :inverse-icon="inverseIcon"
    />
  </span>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { RelationTypeLabel } from "@/models/relationTypeOptions"
import SvgRelationTypeIcon from "../svgs/SvgRelationTypeIcon.vue"

defineProps({
  relationTypeLabel: {
    type: String as PropType<RelationTypeLabel | undefined>,
    default: undefined,
  },
  inverseIcon: Boolean,
})
</script>
