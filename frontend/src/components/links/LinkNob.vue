<template>
  <span class="link-nob" :title="noteTopic.linkType">
    <SvgLinkTypeIcon
      v-if="noteTopic.linkType"
      :link-type="noteTopic.linkType"
      :inverse-icon="inverseIcon"
    />
  </span>
</template>

<script setup lang="ts">
import { NoteTopic } from "@/generated/backend"
import { PropType } from "vue"

defineProps({
  noteTopic: {
    type: Object as PropType<NoteTopic>,
    required: true,
  },
  inverseIcon: Boolean,
})
</script>
