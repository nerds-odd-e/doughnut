<template>
  <div class="daisy-space-y-8">
    <main>
      <div class="daisy-flex daisy-items-center daisy-justify-between daisy-mb-6">
        <h2 class="daisy-text-3xl daisy-font-bold">My Notebooks</h2>
        <NotebookNewButton>Add New Notebook</NotebookNewButton>
      </div>
      <NotebookCardsWithButtons v-if="notebooks.length > 0" :notebooks="notebooks">
        <template #default="{ notebook }">
          <NotebookButtons 
            v-bind="{ notebook, user }" 
            @notebook-updated="handleNotebookUpdated"
          />
        </template>
      </NotebookCardsWithButtons>
      <div v-else class="daisy-alert daisy-alert-info daisy-shadow-sm">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="daisy-h-6 daisy-w-6 daisy-shrink-0 daisy-stroke-current">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>
        <span>You don't have any notebooks yet. Create your first notebook to get started!</span>
      </div>
    </main>
    
    <div class="daisy-divider"></div>
    
    <section>
      <h2 class="daisy-text-3xl daisy-font-bold daisy-mb-6 daisy-flex daisy-items-center daisy-gap-3">
        <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" fill="currentColor" class="daisy-text-info" viewBox="0 0 16 16">
          <path d="M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16m.93-9.412-1 4.705c-.07.34.029.533.304.533.194 0 .487-.07.686-.246l-.088.416c-.287.346-.92.598-1.465.598-.703 0-1.002-.422-.808-1.319l.738-3.468c.064-.293.006-.399-.287-.47l-.451-.081.082-.381 2.29-.287zM8 5.5a1 1 0 1 1 0-2 1 1 0 0 1 0 2"/>
        </svg>
        Subscribed Notebooks
      </h2>
      <NotebookCardsWithButtons v-if="subscriptions.length > 0" :notebooks="subscriptions.map((s) => s.notebook!)" :is-subscribed="true">
        <template #default="{ notebook }">
          <SubscriptionNoteButtons
            v-if="subscriptions.find((s) => s.notebook === notebook)"
            :subscription="subscriptions.find((s) => s.notebook === notebook)!"
            @updated="handleRefresh"
          />
        </template>
      </NotebookCardsWithButtons>
      <div v-else class="daisy-alert daisy-alert-info daisy-shadow-sm">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="daisy-h-6 daisy-w-6 daisy-shrink-0 daisy-stroke-current">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>
        <span>No subscribed notebooks yet. Visit the Bazaar to find notebooks to subscribe to.</span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Notebook, Subscription, User } from "@generated/backend"
import NotebookNewButton from "@/components/notebook/NotebookNewButton.vue"
import NotebookCardsWithButtons from "@/components/notebook/NotebookCardsWithButtons.vue"
import NotebookButtons from "@/components/notebook/NotebookButtons.vue"
import SubscriptionNoteButtons from "@/components/subscriptions/SubscriptionNoteButtons.vue"

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

const handleRefresh = () => {
  emit("refresh")
}
</script>

