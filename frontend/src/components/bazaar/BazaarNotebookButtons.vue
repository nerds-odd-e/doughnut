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
    <PopButton title="Generate assessment questions">
      <template #button_face>
        <SvgAssociation />
      </template>
      <template #default="{ closer }">
        <AssessmentDialog
          v-bind="{ notebook, loggedIn }"
          @close-dialog="closer"
        />
      </template>
    </PopButton>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Notebook } from "@/generated/backend";
import PopButton from "../commons/Popups/PopButton.vue";
import SubscribeDialog from "./SubscribeDialog.vue";
import AssessmentDialog from "./AssessmentDialog.vue";
import SvgAdd from "../svgs/SvgAdd.vue";
import SvgAssociation from "../svgs/SvgAssociation.vue";

export default defineComponent({
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
});
</script>
