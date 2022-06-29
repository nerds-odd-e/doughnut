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
        expandInfo,
      }"
      :key="noteId"
      @level-changed="$emit('levelChanged', $event)"
      @note-deleted="$emit('noteDeleted', $event)"
    />
  </div>

  <div v-if="link">
    <div class="jumbotron py-4 mb-2">
      <LinkShow
        v-bind="{ link }"
        @note-realm-updated="$emit('noteRealmUpdated', $event)"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkShow from "../links/LinkShow.vue";
import NoteCardsView from "../notes/views/NoteCardsView.vue";

export default defineComponent({
  props: {
    reviewPoint: {
      type: Object as PropType<Generated.ReviewPoint>,
      required: true,
    },
    expandInfo: { type: Boolean, default: false },
  },
  emits: ["noteRealmUpdated", "levelChanged", "noteDeleted"],
  components: { LinkShow, NoteCardsView },
  computed: {
    noteId() {
      return this.reviewPoint.thing.note?.id;
    },
    link() {
      return this.reviewPoint.thing.link;
    },
  },
});
</script>
