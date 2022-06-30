<template>
  <div>
    <div v-if="noteInfo.reviewPoint">
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
    </div>

    <div v-if="noteInfo.note">
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
    </div>
    <ReviewSettingForm
      v-bind="{ noteId: noteInfo.note.id, reviewSetting }"
      @level-changed="$emit('levelChanged', $event)"
    />
  </div>
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
