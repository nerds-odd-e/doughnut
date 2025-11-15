<template>
  <table v-if="notebooks" class="daisy-table">
    <tbody>
      <tr v-for="bazaarNotebook in notebooks" :key="bazaarNotebook.id">
        <td>
          <NotebookLink
            :notebook="bazaarNotebook.notebook"
          />
        </td>
        <td>
          <button
            class="daisy-btn daisy-btn-danger"
            @click="removeFromBazaar(bazaarNotebook)"
          >
            Remove
          </button>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script lang="ts" setup>
import { onMounted, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { BazaarNotebook } from "@generated/backend"
import NotebookLink from "../notes/NotebookLink.vue"
import usePopups from "../commons/Popups/usePopups"

const { managedApi } = useLoadingApi()
const { popups } = usePopups()

const notebooks = ref<BazaarNotebook[] | undefined>(undefined)

const fetchData = async () => {
  notebooks.value = await managedApi.services.bazaar()
}

const removeFromBazaar = async (bazaarNotebook: BazaarNotebook) => {
  if (
    await popups.confirm(
      `Are you sure you want to remove "${bazaarNotebook.notebook.title}" from the bazaar?`
    )
  ) {
    notebooks.value = await managedApi.services.removeFromBazaar({
      bazaarNotebook: bazaarNotebook.id!,
    })
  }
}

onMounted(() => {
  fetchData()
})
</script>
