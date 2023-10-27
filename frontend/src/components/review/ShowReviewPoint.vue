<template>
  <div class="alert alert-danger" v-if="reviewPoint.removedFromReview">
    This review point has been removed from reviewing.
  </div>
  <div v-if="noteId">
    <NoteCardsView
      v-if="noteId"
      v-bind="{
        noteId,
        expandChildren: false,
        readonly: false,
        expandInfo,
        storageAccessor,
      }"
      :key="noteId"
      @level-changed="$emit('levelChanged', $event)"
      @self-evaluated="$emit('selfEvaluated', $event)"
    >
      <slot />
    </NoteCardsView>
  </div>

  <div v-if="link">
    <div class="jumbotron py-4 mb-2">
      <LinkShow v-bind="{ link, storageAccessor }" />
    </div>
    <slot />
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkShow from "../links/LinkShow.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

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
  components: { LinkShow },
  computed: {
    noteId() {
      return this.reviewPoint.thing?.note?.id;
    },
    link() {
      return this.reviewPoint.thing.link;
    },
  },
});
</script>
