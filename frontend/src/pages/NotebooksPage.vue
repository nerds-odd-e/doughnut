<template>
  <ContainerPage v-bind="{ contentLoaded: notebooks !== undefined, title: 'Notebooks' }">
    <main class="daisy-mb-8">
      <div class="daisy-flex daisy-items-center daisy-justify-between daisy-mb-6">
        <h2 class="daisy-text-2xl daisy-font-bold">My Notebooks</h2>
        <NotebookNewButton>Add New Notebook</NotebookNewButton>
      </div>
      <NotebookCardsWithButtons v-if="notebooks" :notebooks="notebooks">
        <template #default="{ notebook }">
          <NotebookButtons 
            v-bind="{ notebook, user }" 
            @notebook-updated="handleNotebookUpdated"
          />
        </template>
      </NotebookCardsWithButtons>
    </main>
    
    <section class="subscribed-section">
      <h2 class="daisy-text-2xl daisy-font-bold daisy-mb-6 daisy-flex daisy-items-center daisy-gap-2">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor" class="daisy-text-info" viewBox="0 0 16 16">
          <path d="M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16m.93-9.412-1 4.705c-.07.34.029.533.304.533.194 0 .487-.07.686-.246l-.088.416c-.287.346-.92.598-1.465.598-.703 0-1.002-.422-.808-1.319l.738-3.468c.064-.293.006-.399-.287-.47l-.451-.081.082-.381 2.29-.287zM8 5.5a1 1 0 1 1 0-2 1 1 0 0 1 0 2"/>
        </svg>
        Subscribed Notebooks
      </h2>
      <NotebookCardsWithButtons v-if="subscriptions && subscriptions.length > 0" :notebooks="subscriptions?.map((s) => s.notebook!)" :is-subscribed="true">
        <template #default="{ notebook }">
          <SubscriptionNoteButtons
            v-if="subscriptions?.find((s) => s.notebook === notebook)"
            :subscription="subscriptions.find((s) => s.notebook === notebook)!"
            @updated="fetchData()"
          />
        </template>
      </NotebookCardsWithButtons>
      <p v-else class="daisy-text-base-content/60 daisy-italic">
        No subscribed notebooks yet. Visit the Bazaar to find notebooks to subscribe to.
      </p>
    </section>
  </ContainerPage>
</template>

<script setup lang="ts">
import { inject, onMounted, ref, type Ref } from "vue"
import type { Notebook, Subscription, User } from "@generated/backend"
import { NotebookController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import NotebookNewButton from "@/components/notebook/NotebookNewButton.vue"
import NotebookCardsWithButtons from "@/components/notebook/NotebookCardsWithButtons.vue"
import NotebookButtons from "@/components/notebook/NotebookButtons.vue"
import SubscriptionNoteButtons from "@/components/subscriptions/SubscriptionNoteButtons.vue"
import ContainerPage from "./commons/ContainerPage.vue"

const user = inject<Ref<User | undefined>>("currentUser")
const subscriptions = ref<Subscription[] | undefined>(undefined)
const notebooks = ref<Notebook[] | undefined>(undefined)

const fetchData = async () => {
  const { data: result, error } = await NotebookController.myNotebooks({})
  if (!error) {
    notebooks.value = result!.notebooks
    subscriptions.value = result!.subscriptions
  }
}

const handleNotebookUpdated = (updatedNotebook: Notebook) => {
  if (notebooks.value) {
    const index = notebooks.value.findIndex((n) => n.id === updatedNotebook.id)
    if (index !== -1) {
      notebooks.value[index] = updatedNotebook
    }
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.subscribed-section {
  margin-top: 3rem;
  padding-top: 2rem;
  border-top: 2px solid #e5e7eb;
}
</style>
