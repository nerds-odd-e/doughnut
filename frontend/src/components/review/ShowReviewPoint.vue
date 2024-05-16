<template>
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
import { ReviewPoint } from "@/generated/backend";
import { StorageAccessor } from "../../store/createNoteStorage";
import ShowThing from "./ShowThing.vue";

export default defineComponent({
  props: {
    reviewPoint: {
      type: Object as PropType<ReviewPoint>,
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
