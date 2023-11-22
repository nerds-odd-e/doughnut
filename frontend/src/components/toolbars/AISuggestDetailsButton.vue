<template>
  <a
    :title="'suggest details'"
    class="btn btn-sm"
    role="button"
    @click="suggestDetails(selectedNote.details)"
  >
    <SvgRobot />
    <Popup :show="!!clarificationQuestion" @popupDone="clarificationQuestion = ''">
      <AIClarifyingQuestionDialog :question="clarificationQuestion" />
    </Popup>
  </a>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import SvgRobot from "../svgs/SvgRobot.vue";
import AIClarifyingQuestionDialog from "../notes/AIClarifyingQuestionDialog.vue";
import Popup from "../commons/Popups/Popup.vue";

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
    Popup,
    AIClarifyingQuestionDialog,
  },
  data() {
    return {
      isUnmounted: false,
      clarificationQuestion: "",
    };
  },
  methods: {
    async suggestDetails(prev?: string) {
      const { moreCompleteContent: details, ...response } =
        await this.api.ai.aiNoteDetailsCompletion(this.selectedNote.id, prev);

      // eslint-disable-next-line
      console.log(response);

      if (this.isUnmounted) return;

      if (response.question) {
        this.clarificationQuestion = response.question;
        return;
      }

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
