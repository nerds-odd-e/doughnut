<template>
  <div class="btn-group btn-group-sm">
    <PopButton
      v-if="!notebook.notebookSettings.skipReviewEntirely"
      title="Add to my learning"
    >
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
  <div v-if="certifiedNotebook" class="p-1 certification-icon" >
    <SvgCertifiedAssessment/>
  </div>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue"

import { Notebook } from "@/generated/backend"
import PopButton from "../commons/Popups/PopButton.vue"
import usePopups from "../commons/Popups/usePopups"
import SvgAdd from "../svgs/SvgAdd.vue"
import SvgAssessment from "../svgs/SvgAssessment.vue"
import SubscribeDialog from "./SubscribeDialog.vue"
import SvgCertifiedAssessment from "../svgs/SvgCertifiedAssessment.vue"

export default defineComponent({
  setup() {
    return usePopups()
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
    SvgCertifiedAssessment,
  },
  computed: {
    certifiedNotebook() {
      return this.notebook.approvalStatus === "APPROVED"
    },
  },
  methods: {
    openAssessmentPage() {
      if (!this.loggedIn) {
        this.popups.alert("Please login first")
        return
      }
      this.$router.push({
        name: "assessment",
        query: {
          topic: this.notebook.headNote.noteTopic.topicConstructor,
          approvalStatus: this.notebook.approvalStatus,
        },
        params: {
          notebookId: this.notebook.id,
        },
      })
    },
  },
})
</script>
