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
    <PopButton title="Start Assessment">
      <template #button_face>
        <SvgChat />
      </template>
      <OnlineAssessmentDialog v-if="loggedIn" />
    </PopButton>
    <PopButton title="Generate assessment questions">
      <template #button_face>
        <SvgAssociation @click="generateAssessmentQuestions" />
      </template>

      <template #default="{ closer }">
        <AssessmentDialog
          v-if="!loggedIn"
          @close-dialog="closer"
          error-message="Please login first"
        />
        <AssessmentDialog
          v-if="loggedIn && noAssessmentQuestions"
          @close-dialog="closer"
          error-message="Insufficient notes to create assessment!"
        />
      </template>
    </PopButton>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { Notebook } from "@/generated/backend";
import PopButton from "../commons/Popups/PopButton.vue";
import SubscribeDialog from "./SubscribeDialog.vue";
import AssessmentDialog from "./AssessmentDialog.vue";
import OnlineAssessmentDialog from "../notes/OnlineAssessmentDialog.vue";
import SvgAdd from "../svgs/SvgAdd.vue";
import SvgChat from "../svgs/SvgChat.vue";
import SvgAssociation from "../svgs/SvgAssociation.vue";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  props: {
    notebook: { type: Object as PropType<Notebook>, required: true },
    loggedIn: Boolean,
  },
  components: {
    PopButton,
    SvgAdd,
    SvgAssociation,
    SubscribeDialog,
    AssessmentDialog,
  },
  data() {
    return {
      noAssessmentQuestions: false,
    };
  },
  methods: {
    generateAssessmentQuestions() {
      if (!this.loggedIn) {
        return;
      }
      this.managedApi.restAssessmentController
        .generateAiQuestions(this.notebook.id)
        .then((response) => {
          if (!response || response.length === 0) {
            this.noAssessmentQuestions = true;
          }
          this.$router.push({ name: "assessment" });
        });
    },
  },
});
</script>
