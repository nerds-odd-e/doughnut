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
import { BazaarController } from "@generated/backend/sdk.gen"
import type { BazaarNotebook } from "@generated/backend"
import NotebookLink from "../notes/NotebookLink.vue"
import usePopups from "../commons/Popups/usePopups"

const { popups } = usePopups()

const notebooks = ref<BazaarNotebook[] | undefined>(undefined)

const fetchData = async () => {
  const { data: bazaarNotebooks, error } = await BazaarController.bazaar()
  if (!error) {
    notebooks.value = bazaarNotebooks!
  }
}

const removeFromBazaar = async (bazaarNotebook: BazaarNotebook) => {
  if (
    await popups.confirm(
      `Are you sure you want to remove "${bazaarNotebook.notebook.title}" from the bazaar?`
    )
  ) {
    const { data: updatedNotebooks, error } =
      await BazaarController.removeFromBazaar({
        path: { bazaarNotebook: bazaarNotebook.id! },
      })
    if (!error) {
      notebooks.value = updatedNotebooks!
    }
  }
}

onMounted(() => {
  fetchData()
})
</script>
