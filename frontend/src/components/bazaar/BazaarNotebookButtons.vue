<template>
  <div class="btn-group btn-group-sm">
    <PopupButton
      v-if="!notebook.skipReviewEntirely"
      title="Add to my learning"
      :icon="SvgAdd"
    >
    <template v-slot:face>
      <SvgAdd/>
    </template>
      <SubscribeDialog v-bind="{notebook, user}"/>
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
  user: Boolean });
</script>
