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
import { PropType } from "vue";
import { NoteTopic } from "@/generated/backend";
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";

defineProps({
  noteTopic: {
    type: Object as PropType<NoteTopic>,
    required: true,
  },
  inverseIcon: Boolean,
});
</script>
