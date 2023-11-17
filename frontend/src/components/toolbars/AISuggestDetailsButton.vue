<template>
  <a
    :title="'suggest details'"
    class="btn btn-sm"
    role="button"
    @click="suggestDetails(selectedNote.details)"
  >
    <SvgRobot />
  </a>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import SvgRobot from "../svgs/SvgRobot.vue";

export default defineComponent({
  setup() {
    return {
      ...useLoadingApi(),
    };
  },
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    selectedNote: {
      type: Object as PropType<Generated.Note>,
      required: true,
    },
  },
  components: {
    SvgRobot,
  },
  data() {
    return {
      isUnmounted: false,
    };
  },
  methods: {
    async suggestDetails(prev?: string) {
      const details = await this.api.ai.aiNoteDetailsCompletion(
        this.selectedNote.id,
        prev,
      );

      if (this.isUnmounted) return;

      this.storageAccessor.api(this.$router).updateTextContent(
        this.selectedNote.id,
        {
          topic: this.selectedNote.topic,
          details,
        },
        {
          topic: this.selectedNote.topic,
          details: this.selectedNote.details,
        },
      );
    },
  },
  unmounted() {
    this.isUnmounted = true;
  },
});
</script>
