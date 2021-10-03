<template>
  <div class="row">
    <div class="col-12 col-md-4 link-source">
      <LinkNoteShow
        :noteViewedByUser="sourceNoteWithPosition"
      />
    </div>
    <div class="col-12 col-md-4 text-center">
      <slot />
    </div>
    <div class="col-12 col-md-4 link-target">
      <LinkNoteShow
        :noteViewedByUser="targetNoteWithPosition"
      />
    </div>
  </div>
</template>

<script>
import LinkNoteShow from "./LinkNoteShow.vue";

export default {
  props: {
    sourceNoteWithPosition: { tyep: Object, required: true },
    targetNoteWithPosition: { type: Object, required: true },
  },
  components: { LinkNoteShow },
};
</script>
