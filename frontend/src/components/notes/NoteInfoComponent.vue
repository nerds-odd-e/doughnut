<template>
  <h6>Review Settings</h6>
  <ReviewSettingForm
    v-bind="{ noteId: noteInfo.note.id, reviewSetting }"
    @level-changed="$emit('levelChanged', $event)"
  />
  <template v-if="memoryTracker">
    <h6>Memory Tracker</h6>
    <NoteInfoMemoryTracker
      v-model="memoryTracker"
      @update:model-value="onSelfEvaluated($event)"
    />
  </template>
</template>

<script lang="ts">
import type { NoteInfo, MemoryTracker } from "@/generated/backend"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import ReviewSettingForm from "../review/ReviewSettingForm.vue"
import NoteInfoMemoryTracker from "./NoteInfoMemoryTracker.vue"

export default defineComponent({
  props: {
    noteInfo: { type: Object as PropType<NoteInfo>, required: true },
  },
  emits: ["levelChanged"],
  data() {
    return {
      memoryTracker: this.noteInfo.memoryTracker,
    }
  },
  computed: {
    reviewSetting() {
      return this.noteInfo.reviewSetting
    },
  },
  components: { ReviewSettingForm, NoteInfoMemoryTracker },
  methods: {
    onSelfEvaluated(memoryTracker: MemoryTracker) {
      this.memoryTracker = memoryTracker
    },
  },
})
</script>

<style lang="scss" scoped>
ul {
  margin-bottom: 0;
}
</style>
