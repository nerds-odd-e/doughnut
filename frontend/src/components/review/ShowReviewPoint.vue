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

  <div v-if="link && linkViewedByUser">
    <div class="jumbotron py-4 mb-2">
      <LinkShow
        v-bind="{
          sourceNote: link.sourceNote,
          targetNote: link.targetNote,
        }"
      >
        <LinkNob
          v-bind="{ link }"
          @note-realm-updated="$emit('noteRealmUpdated', $event)"
        />
        <span class="badge bg-light text-dark">
          {{ linkViewedByUser.linkTypeLabel }}</span
        >
      </LinkShow>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteRealmAsync from "../notes/NoteRealmAsync.vue";
import LinkShow from "../links/LinkShow.vue";
import LinkNob from "../links/LinkNob.vue";

export default defineComponent({
  props: {
    reviewPointViewedByUser: {
      type: Object as PropType<Generated.ReviewPointViewedByUser>,
      required: true,
    },
  },
  components: { LinkShow, LinkNob, NoteRealmAsync },
  computed: {
    noteId() {
      return this.reviewPoint?.thing?.note?.id;
    },
    reviewPoint() {
      return this.reviewPointViewedByUser.reviewPoint;
    },
    link() {
      return this.linkViewedByUser?.link;
    },
    linkViewedByUser() {
      return this.reviewPointViewedByUser?.linkViewedByUser;
    },
  },
});
</script>
