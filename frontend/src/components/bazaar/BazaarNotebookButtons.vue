<template>
  <div class="btn-group btn-group-sm">
    <PopButton v-if="!notebook.skipReviewEntirely" title="Add to my learning">
      <template #button_face>
        <SvgAdd />
      </template>
      <template #default="{ closer }">
        <SubscribeDialog
          v-bind="{ notebook, loggedIn }"
          @close-dialog="closer"
        />
      </template>
    </PopButton>
    <button class="btn" title="Start Assessment" @click="openAssessmentPage">
      <SvgAssessment />
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";

import { Notebook } from "@/generated/backend";
import PopButton from "../commons/Popups/PopButton.vue";
import SubscribeDialog from "./SubscribeDialog.vue";
import SvgAdd from "../svgs/SvgAdd.vue";
import SvgAssessment from "../svgs/SvgAssessment.vue";
import usePopups from "../commons/Popups/usePopups";

export default defineComponent({
  setup() {
    return usePopups();
  },
  props: {
    notebook: { type: Object as PropType<Notebook>, required: true },
    loggedIn: Boolean,
  },
  components: {
    PopButton,
    SvgAdd,
    SvgAssessment,
    SubscribeDialog,
  },
  methods: {
    openAssessmentPage() {
      if (!this.loggedIn) {
        this.popups.alert("Please login first");
        return;
      }
      this.$router.push({
        name: "assessment",
        query: {
          topic: this.notebook.headNote.noteTopic.topicConstructor,
        },
        params: {
          notebookId: this.notebook.id,
        },
      });
    },
  },
});
</script>
