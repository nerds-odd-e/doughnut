<template>
  <ContainerPage v-bind="{ contentLoaded: notebooks !== undefined, title: 'Notebooks' }">
    <p class="daisy-mb-6">
      <NotebookNewButton>Add New Notebook</NotebookNewButton>
    </p>
    <main>
      <NotebookCardsWithButtons v-if="notebooks" :notebooks="notebooks">
        <template #default="{ notebook }">
          <NotebookButtons v-bind="{ notebook, user }" />
        </template>
      </NotebookCardsWithButtons>
    </main>
    <h2>Subscribed Notes</h2>
    <NotebookCardsWithButtons v-if="subscriptions" :notebooks="subscriptions?.map((s) => s.notebook!)">
      <template #default="{ notebook }">
        <SubscriptionNoteButtons
          :subscription="subscriptions.find((s) => s.notebook === notebook)"
          @updated="fetchData()"
        />
      </template>
    </NotebookCardsWithButtons>
  </ContainerPage>
</template>

<script setup lang="ts">
import { inject, onMounted, ref, type Ref } from "vue"
import type { Notebook, Subscription, User } from "generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NotebookNewButton from "@/components/notebook/NotebookNewButton.vue"
import NotebookCardsWithButtons from "@/components/notebook/NotebookCardsWithButtons.vue"
import NotebookButtons from "@/components/notebook/NotebookButtons.vue"
import SubscriptionNoteButtons from "@/components/subscriptions/SubscriptionNoteButtons.vue"
import ContainerPage from "./commons/ContainerPage.vue"

const { managedApi } = useLoadingApi()

const user = inject<Ref<User | undefined>>("currentUser")
const subscriptions = ref<Subscription[] | undefined>(undefined)
const notebooks = ref<Notebook[] | undefined>(undefined)

const fetchData = async () => {
  const res = await managedApi.restNotebookController.myNotebooks()
  notebooks.value = res.notebooks
  subscriptions.value = res.subscriptions
}
onMounted(() => {
  fetchData()
})
</script>
