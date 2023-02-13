<template>
  <div class="btn-group btn-group-sm">
    <PopButton v-if="!notebook.skipReviewEntirely" title="Add to my learning">
      <template #button_face>
        <SvgAdd />
      </template>
      <SubscribeDialog v-bind="{ notebook, loggedIn }" />
    </PopButton>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import PopButton from "../commons/Popups/PopButton.vue";
import SubscribeDialog from "./SubscribeDialog.vue";
import SvgAdd from "../svgs/SvgAdd.vue";

export default defineComponent({
  props: {
    notebook: { type: Object as PropType<Generated.Notebook>, required: true },
    loggedIn: Boolean,
  },
  components: {
    PopButton,
    SvgAdd,
    SubscribeDialog,
  },
});
</script>
