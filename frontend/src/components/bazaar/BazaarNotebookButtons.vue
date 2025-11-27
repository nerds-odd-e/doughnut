<template>
  <div class="daisy-btn-group daisy-btn-group-sm">
    <PopButton
      v-if="!notebook.notebookSettings.skipMemoryTrackingEntirely"
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
    <button
      class="daisy-btn daisy-btn-ghost daisy-btn-sm"
      title="Start Assessment"
      @click="openAssessmentPage"
    >
      <SvgAssessment />
    </button>
  </div>
  <div v-if="notebook.certifiable" class="daisy-p-1 certification-icon" >
    <SvgCertifiedAssessment/>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { useRouter } from "vue-router"
import type { Notebook } from "@generated/backend"
import PopButton from "../commons/Popups/PopButton.vue"
import usePopups from "../commons/Popups/usePopups"
import SvgAdd from "../svgs/SvgAdd.vue"
import SvgAssessment from "../svgs/SvgAssessment.vue"
import SubscribeDialog from "./SubscribeDialog.vue"
import SvgCertifiedAssessment from "../svgs/SvgCertifiedAssessment.vue"

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  loggedIn: Boolean,
})

const { popups } = usePopups()
const router = useRouter()

const openAssessmentPage = () => {
  if (!props.loggedIn) {
    popups.alert("You need to be logged in to start an assessment.")
    return
  }
  router.push({
    name: "assessment",
    params: { notebookId: props.notebook.id },
  })
}
</script>
