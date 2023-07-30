<template>
  <ul>
    <li v-if="noteInfo.note">
      <h6>Note</h6>
      <label
        >Created:
        <span class="statistics-value">{{
          new Date(noteInfo.createdAt).toLocaleString()
        }}</span></label
      >
      <label
        >Last Content Updated:
        <span class="statistics-value">{{
          new Date(noteInfo.note.note.updatedAt).toLocaleString()
        }}</span></label
      >
    </li>
    <li>
      <h6>Review Settings</h6>
      <ReviewSettingForm
        v-bind="{ noteId: noteInfo.note.id, reviewSetting }"
        @level-changed="$emit('levelChanged', $event)"
      />
    </li>
    <li v-if="reviewPoint">
      <h6>Review Point</h6>
      <NoteInfoReviewPoint
        v-bind="{ reviewPoint }"
        @self-evaluated="onSelfEvaluated($event)"
      />
    </li>
  </ul>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ReviewSettingForm from "../review/ReviewSettingForm.vue";
import NoteInfoReviewPoint from "./NoteInfoReviewPoint.vue";

export default defineComponent({
  props: {
    noteInfo: { type: Object as PropType<Generated.NoteInfo>, required: true },
  },
  emits: ["levelChanged", "selfEvaluated"],
  data() {
    return {
      reviewPoint: this.noteInfo.reviewPoint,
    };
  },
  computed: {
    reviewSetting() {
      return this.noteInfo.reviewSetting;
    },
  },
  components: { ReviewSettingForm, NoteInfoReviewPoint },
  methods: {
    onSelfEvaluated(reviewPoint: Generated.ReviewPoint) {
      this.reviewPoint = reviewPoint;
      this.$emit("selfEvaluated", reviewPoint);
    },
  },
});
</script>

<style lang="scss" scoped>
ul {
  margin-bottom: 0;
}
</style>
