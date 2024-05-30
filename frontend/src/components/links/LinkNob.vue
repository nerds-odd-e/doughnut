<template>
  <span class="link-nob">
    <SvgLinkTypeIcon
      v-if="note.linkType"
      :link-type="note.linkType"
      :inverse-icon="inverseIcon"
    />
  </span>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";

export default defineComponent({
  props: {
    note: {
      type: Object as PropType<Note>,
      required: true,
    },
    inverseIcon: Boolean,
  },
  components: { SvgLinkTypeIcon },
});
</script>
