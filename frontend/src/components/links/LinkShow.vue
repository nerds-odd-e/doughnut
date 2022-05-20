<template>
  <div class="row">
    <div class="col-12 col-md-4 link-source">
      <LinkNoteShow :note="sourceNote" />
    </div>
    <div class="col-12 col-md-4 text-center">
      <slot />
    </div>
    <div class="col-12 col-md-4 link-target">
      <LinkNoteShow :note="targetNote" />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkNoteShow from "./LinkNoteShow.vue";

export default defineComponent({
  props: {
    sourceNote: {
      type: Object as PropType<Generated.Note>,
      required: true,
    },
    targetNote: {
      type: Object as PropType<Generated.Note>,
      required: true,
    },
  },
  components: { LinkNoteShow },
});
</script>
