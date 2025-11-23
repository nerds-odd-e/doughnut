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
import { bazaar } from "@generated/backend/sdk.gen"
import NotebookBazaarViewCards from "@/components/bazaar/NotebookBazaarViewCards.vue"
import ContainerPage from "./commons/ContainerPage.vue"

const user = inject<Ref<User | undefined>>("currentUser")

const bazaarNotebooks = ref<BazaarNotebook[] | undefined>(undefined)

const fetchData = async () => {
  const { data: notebooks, error } = await bazaar()
  if (!error) {
    // notebooks is guaranteed to be BazaarNotebook[] when error is undefined
    bazaarNotebooks.value = notebooks!
  }
}

onMounted(() => {
  fetchData()
})
</script>
