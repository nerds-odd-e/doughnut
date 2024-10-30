<template>
  <ContainerPage v-bind="{ contentLoaded: notebooks !== undefined, title: 'Notebooks' }">
    <p>
      <NotebookNewButton>Add New Notebook</NotebookNewButton>
    </p>
    <main>
      <NotebookViewCards v-if="notebooks" :notebooks="notebooks" :user="user" />
    </main>
    <h2>Subscribed Notes</h2>
    <NotebookSubscriptionCards
      :subscriptions="subscriptions"
      @updated="fetchData()"
    />
  </ContainerPage>
</template>

<script setup lang="ts">
import { inject, onMounted, ref, type Ref } from "vue"
import type { Notebook, Subscription, User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NotebookNewButton from "@/components/notebook/NotebookNewButton.vue"
import NotebookViewCards from "@/components/notebook/NotebookViewCards.vue"
import NotebookSubscriptionCards from "@/components/subscriptions/NotebookSubscriptionCards.vue"
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
