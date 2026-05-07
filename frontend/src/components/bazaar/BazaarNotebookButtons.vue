<template>
  <div class="daisy-btn-group daisy-btn-group-sm">
    <PopButton
      v-if="!notebook.notebookSettings.skipMemoryTrackingEntirely"
      title="Add to my learning"
    >
      <template #button_face>
        <Plus class="daisy-w-6 daisy-h-6" />
      </template>
      <template #default="{ closer }">
        <SubscribeDialog
          v-bind="{ notebook, loggedIn }"
          @close-dialog="closer"
        />
      </template>
    </PopButton>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Notebook } from "@generated/doughnut-backend-api"
import PopButton from "../commons/Popups/PopButton.vue"
import { Plus } from "lucide-vue-next"
import SubscribeDialog from "./SubscribeDialog.vue"

defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  loggedIn: Boolean,
})
</script>
