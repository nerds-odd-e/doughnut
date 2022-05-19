<template>
  <div class="btn-group btn-group-sm">
    <PopupButton v-if="!notebook.skipReviewEntirely" title="Add to my learning">
      <template #button_face>
        <SvgAdd />
      </template>
      <template #dialog_body="{ doneHandler }">
        <SubscribeDialog
          v-bind="{ notebook, user }"
          @done="doneHandler($event)"
        />
      </template>
    </PopupButton>
  </div>
</template>

<script setup lang="ts">
import { PropType } from "vue";
import PopupButton from "../commons/Popups/PopupButton.vue";
import SubscribeDialog from "./SubscribeDialog.vue";
import SvgAdd from "../svgs/SvgAdd.vue";

const props = defineProps({
  notebook: { type: Object as PropType<Generated.Notebook>, required: true },
  user: Object,
});
</script>
