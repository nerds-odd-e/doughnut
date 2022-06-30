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
          new Date(noteInfo.note.note.textContent.updatedAt).toLocaleString()
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
    <li v-if="noteInfo.reviewPoint">
      <h6>Review Point</h6>
      <label
        >Repetition Count:
        <span class="statistics-value">{{
          noteInfo.reviewPoint.repetitionCount
        }}</span></label
      >
      <label
        >Forgetting Curive Index:
        <span class="statistics-value">{{
          noteInfo.reviewPoint.forgettingCurveIndex
        }}</span></label
      >
      <label
        >Next Review:
        <span class="statistics-value">{{
          new Date(noteInfo.reviewPoint.nextReviewAt).toLocaleString()
        }}</span></label
      >
    </li>
  </ul>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ReviewSettingForm from "../review/ReviewSettingForm.vue";

export default defineComponent({
  props: {
    noteInfo: { type: Object as PropType<Generated.NoteInfo>, required: true },
  },
  emits: ["levelChanged"],
  computed: {
    reviewSetting() {
      return this.noteInfo.reviewSetting;
    },
  },
  components: { ReviewSettingForm },
});
</script>

<style lang="scss" scoped>
ul {
  margin-bottom: 0;
}
</style>
