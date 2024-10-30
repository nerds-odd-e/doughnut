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
        :logged-in="!!props.user"
      />
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import type { BazaarNotebook, User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NotebookBazaarViewCards from "@/components/bazaar/NotebookBazaarViewCards.vue"
import ContainerPage from "./commons/ContainerPage.vue"

const props = defineProps<{
  user?: User
}>()

const { managedApi } = useLoadingApi()
const bazaarNotebooks = ref<BazaarNotebook[] | undefined>(undefined)

const fetchData = () => {
  managedApi.restBazaarController.bazaar().then((res) => {
    bazaarNotebooks.value = res
  })
}

onMounted(() => {
  fetchData()
})
</script>
