<template>
  <div class="alert alert-danger" v-if="reviewPoint.removedFromReview">
    This review point has been removed from reviewing.
  </div>
  <ShowThing
    v-bind="{ thing: reviewPoint.thing, expandInfo, storageAccessor }"
    @level-changed="$emit('levelChanged', $event)"
    @self-evaluated="$emit('selfEvaluated', $event)"
  >
    <slot />
  </ShowThing>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import ShowThing from "./ShowThing.vue";

export default defineComponent({
  props: {
    reviewPoint: {
      type: Object as PropType<Generated.ReviewPoint>,
      required: true,
    },
    expandInfo: { type: Boolean, default: false },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["levelChanged", "selfEvaluated"],
  components: { ShowThing },
});
</script>
