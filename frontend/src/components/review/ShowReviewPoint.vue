<template>
  <div class="alert alert-danger" v-if="reviewPoint.removedFromReview">
    This review point has been removed from reviewing.
  </div>
  <div v-if="noteId">
    <NoteRealmAsync
      v-if="noteId"
      v-bind="{
        noteId,
        expandChildren: false,
        viewType: 'cards',
      }"
      :key="noteId"
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
import NoteRealmAsync from "../notes/NoteRealmAsync.vue";
import LinkShow from "../links/LinkShow.vue";

export default defineComponent({
  props: {
    reviewPointViewedByUser: {
      type: Object as PropType<Generated.ReviewPointViewedByUser>,
      required: true,
    },
  },
  emits: ["noteRealmUpdated"],
  components: { LinkShow,  NoteRealmAsync },
  computed: {
    noteId() {
      return this.reviewPoint?.thing?.note?.id;
    },
    reviewPoint() {
      return this.reviewPointViewedByUser.reviewPoint;
    },
    link() {
      return this.reviewPoint?.thing.link;
    },
  },
});
</script>
