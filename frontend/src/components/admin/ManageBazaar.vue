<template>
  <table v-if="notebooks" class="table">
    <tbody>
      <tr v-for="bazaarNotebook in notebooks" :key="bazaarNotebook.id">
        <td>
          <NoteTopicWithLink
            v-bind="{ noteTopic: bazaarNotebook.notebook.headNote.noteTopic }"
          />
        </td>
        <td>
          <button
            class="btn btn-dange"
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
import type { BazaarNotebook } from "@/generated/backend"
import NoteTopicWithLink from "../notes/NoteTopicWithLink.vue"
import usePopups from "../commons/Popups/usePopups"

const { managedApi } = useLoadingApi()
const { popups } = usePopups()

const notebooks = ref<BazaarNotebook[] | undefined>(undefined)

const fetchData = async () => {
  notebooks.value = await managedApi.restBazaarController.bazaar()
}

const removeFromBazaar = async (bazaarNotebook: BazaarNotebook) => {
  if (
    await popups.confirm(
      `Are you sure you want to remove "${bazaarNotebook.notebook.headNote.noteTopic.topicConstructor}" from the bazaar?`
    )
  ) {
    notebooks.value = await managedApi.restBazaarController.removeFromBazaar(
      bazaarNotebook.id!
    )
  }
}

onMounted(() => {
  fetchData()
})
</script>
