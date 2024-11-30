<template>
  <h6>Review Settings</h6>
  <ReviewSettingForm
    v-bind="{ noteId: noteInfo.note.id, reviewSetting }"
    @level-changed="$emit('levelChanged', $event)"
  />
  <template v-if="reviewPoint">
    <h6>Memory Tracker</h6>
    <NoteInfoReviewPoint
      v-model="reviewPoint"
      @update:model-value="onSelfEvaluated($event)"
    />
  </template>
</template>

<script lang="ts">
import type { NoteInfo, MemoryTracker } from "@/generated/backend"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import ReviewSettingForm from "../review/ReviewSettingForm.vue"
import NoteInfoReviewPoint from "./NoteInfoReviewPoint.vue"

export default defineComponent({
  props: {
    noteInfo: { type: Object as PropType<NoteInfo>, required: true },
  },
  emits: ["levelChanged"],
  data() {
    return {
      reviewPoint: this.noteInfo.reviewPoint,
    }
  },
  computed: {
    reviewSetting() {
      return this.noteInfo.reviewSetting
    },
  },
  components: { ReviewSettingForm, NoteInfoReviewPoint },
  methods: {
    onSelfEvaluated(reviewPoint: MemoryTracker) {
      this.reviewPoint = reviewPoint
    },
  },
})
</script>

<style lang="scss" scoped>
ul {
  margin-bottom: 0;
}
</style>
