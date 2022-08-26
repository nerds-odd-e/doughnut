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
        historyWriter,
      }"
      :key="noteId"
      @level-changed="$emit('levelChanged', $event)"
      @self-evaluated="$emit('selfEvaluated', $event)"
    />
  </div>

  <div v-if="link">
    <div class="jumbotron py-4 mb-2">
      <LinkShow
        v-bind="{ link }"
        @link-updated="$emit('noteRealmUpdated', $event)"
        @link-deleted="$emit('linkDeleted', $event)"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkShow from "../links/LinkShow.vue";
import NoteCardsView from "../notes/views/NoteCardsView.vue";
import { HistoryWriter } from "../../store/history";

export default defineComponent({
  props: {
    reviewPoint: {
      type: Object as PropType<Generated.ReviewPoint>,
      required: true,
    },
    expandInfo: { type: Boolean, default: false },
    historyWriter: {
      type: Function as PropType<HistoryWriter>,
      required: true,
    },
  },
  emits: ["noteRealmUpdated", "levelChanged", "linkDeleted", "selfEvaluated"],
  components: { LinkShow, NoteCardsView },
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
