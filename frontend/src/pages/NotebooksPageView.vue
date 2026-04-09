<template>
  <GlobalBar>
    <template #right>
      <NotebookNewButton v-if="user">Add New Notebook</NotebookNewButton>
    </template>
  </GlobalBar>
  <div class="daisy-container daisy-mx-auto daisy-max-w-3xl daisy-px-4 daisy-py-6">
    <section class="daisy-mb-12">
      <div class="daisy-mb-5 daisy-flex daisy-flex-col daisy-gap-1 sm:daisy-flex-row sm:daisy-items-end sm:daisy-justify-between">
        <div>
          <h1 class="daisy-text-2xl daisy-font-bold daisy-tracking-tight daisy-text-base-content">
            My notebooks
          </h1>
          <p class="daisy-mt-1 daisy-text-sm daisy-text-base-content/60">
            Open a notebook to work with notes, or use the toolbar to create a new one.
          </p>
        </div>
      </div>
      <NotebookCardsWithButtons
        v-if="notebooks.length > 0"
        layout="list"
        :notebooks="notebooks"
      >
        <template #default="{ notebook }">
          <NotebookButtons
            v-bind="{ notebook, user }"
            @notebook-updated="handleNotebookUpdated"
          />
        </template>
      </NotebookCardsWithButtons>
      <div
        v-else
        class="daisy-rounded-box daisy-border daisy-border-dashed daisy-border-base-300 daisy-bg-base-200/30 daisy-px-6 daisy-py-10 daisy-text-center"
      >
        <p class="daisy-m-0 daisy-text-base daisy-text-base-content/70">
          You do not have any notebooks yet.
        </p>
        <p class="daisy-mt-2 daisy-mb-0 daisy-text-sm daisy-text-base-content/50">
          Use <span class="daisy-font-medium daisy-text-base-content/80">Add New Notebook</span> above to create your first one.
        </p>
      </div>
    </section>

    <section class="subscribed-section daisy-border-t daisy-border-base-300 daisy-pt-10">
      <h2 class="daisy-mb-1 daisy-text-lg daisy-font-semibold daisy-text-base-content">
        Subscribed notebooks
      </h2>
      <p class="daisy-mb-5 daisy-text-sm daisy-text-base-content/60">
        Notebooks you follow from the Bazaar appear here.
      </p>
      <NotebookCardsWithButtons
        v-if="subscriptions.length > 0"
        layout="list"
        :notebooks="subscriptions.map((s) => s.notebook!)"
        :is-subscribed="true"
      >
        <template #default="{ notebook }">
          <SubscriptionNoteButtons
            v-if="subscriptions.find((s) => s.notebook === notebook)"
            :subscription="subscriptions.find((s) => s.notebook === notebook)!"
            @updated="$emit('refresh')"
          />
        </template>
      </NotebookCardsWithButtons>
      <p v-else class="daisy-m-0 daisy-text-sm daisy-text-base-content/55">
        None yet — visit the Bazaar to subscribe to shared notebooks.
      </p>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type {
  Notebook,
  Subscription,
  User,
} from "@generated/doughnut-backend-api"
import NotebookNewButton from "@/components/notebook/NotebookNewButton.vue"
import NotebookCardsWithButtons from "@/components/notebook/NotebookCardsWithButtons.vue"
import NotebookButtons from "@/components/notebook/NotebookButtons.vue"
import SubscriptionNoteButtons from "@/components/subscriptions/SubscriptionNoteButtons.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"

defineProps({
  notebooks: {
    type: Array as PropType<Notebook[]>,
    required: true,
  },
  subscriptions: {
    type: Array as PropType<Subscription[]>,
    required: true,
  },
  user: {
    type: Object as PropType<User>,
    required: true,
  },
})

const emit = defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
  (e: "refresh"): void
}>()

const handleNotebookUpdated = (updatedNotebook: Notebook) => {
  emit("notebook-updated", updatedNotebook)
}
</script>

