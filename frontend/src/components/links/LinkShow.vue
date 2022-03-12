<template>
  <div class="row">
    <div class="col-12 col-md-4 link-source">
      <LinkNoteShow
        v-bind="sourceNoteWithPosition"
      />
    </div>
    <div class="col-12 col-md-4 text-center">
      <slot />
    </div>
    <div class="col-12 col-md-4 link-target">
      <LinkNoteShow
        v-bind="targetNoteWithPosition"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkNoteShow from "./LinkNoteShow.vue";

export default defineComponent({
  props: {
    sourceNoteWithPosition: { tyep: Object as PropType<Generated.NoteWithPosition>, required: true },
    targetNoteWithPosition: { type: Object as PropType<Generated.NoteWithPosition>, required: true },
  },
  components: { LinkNoteShow },
});
</script>
