<template>
  <div class="btn-group btn-group-sm">
    <PopupButton v-if="!notebook.skipReviewEntirely" title="Add to my learning">
      <template #button_face>
        <SvgAdd />
      </template>
      <template #dialog_body="{ doneHandler }">
        <SubscribeDialog
          v-bind="{ notebook, loggedIn }"
          @done="doneHandler($event)"
        />
      </template>
    </PopupButton>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import PopupButton from "../commons/Popups/PopupButton.vue";
import SubscribeDialog from "./SubscribeDialog.vue";
import SvgAdd from "../svgs/SvgAdd.vue";

export default defineComponent({
  props: {
    notebook: { type: Object as PropType<Generated.Notebook>, required: true },
    loggedIn: Boolean,
  },
  components: {
    PopupButton,
    SvgAdd,
    SubscribeDialog,
  },
});
</script>
