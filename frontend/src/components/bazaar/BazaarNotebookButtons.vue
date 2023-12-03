<template>
  <div class="btn-group btn-group-sm">
    <PopButton1 v-if="!notebook.skipReviewEntirely" title="Add to my learning">
      <template #button_face>
        <SvgAdd />
      </template>
      <template #default="{ closer }">
        <SubscribeDialog
          v-bind="{ notebook, loggedIn }"
          @close-dialog="closer"
        />
      </template>
    </PopButton1>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import PopButton1 from "../commons/Popups/PopButton1.vue";
import SubscribeDialog from "./SubscribeDialog.vue";
import SvgAdd from "../svgs/SvgAdd.vue";

export default defineComponent({
  props: {
    notebook: { type: Object as PropType<Generated.Notebook>, required: true },
    loggedIn: Boolean,
  },
  components: {
    PopButton1,
    SvgAdd,
    SubscribeDialog,
  },
});
</script>
