<template>
  <ContainerPage
    v-bind="{
      contentLoaded: bazaarNotebooks !== undefined,
      title: 'Welcome To The Bazaar',
    }"
  >
    <p>These are shared notes from doughnut users.</p>
    <div v-if="bazaarNotebooks">
      <NotebookBazaarViewCards
        :bazaar-notebooks="bazaarNotebooks"
        :logged-in="!!user"
      />
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import { ref, onMounted, inject, type Ref } from "vue"
import type { BazaarNotebook, User } from "@generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NotebookBazaarViewCards from "@/components/bazaar/NotebookBazaarViewCards.vue"
import ContainerPage from "./commons/ContainerPage.vue"

const user = inject<Ref<User | undefined>>("currentUser")

const { managedApi } = useLoadingApi()
const bazaarNotebooks = ref<BazaarNotebook[] | undefined>(undefined)

const fetchData = () => {
  managedApi.services.bazaar().then((res) => {
    bazaarNotebooks.value = res
  })
}

onMounted(() => {
  fetchData()
})
</script>
