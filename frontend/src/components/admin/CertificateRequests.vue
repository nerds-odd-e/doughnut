<template>
  <table class="table">
    <tbody v-if="notebooks?.length">
      <tr v-for="bazaarNotebook in notebooks" :key="bazaarNotebook.id">
        <td>
          <NoteTopicWithLink
            v-bind="{ noteTopic: bazaarNotebook.notebook.headNote.noteTopic }"
          />
        </td>
        <td>
          <button
            class="btn btn-dange"
          >
            Approve
          </button>
        </td>
      </tr>
    </tbody>
    <h2 v-else>No pending approvals</h2>
  </table>
</template>

<script lang="ts" setup>
import { onMounted, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { BazaarNotebook } from "@/generated/backend"
import NoteTopicWithLink from "../notes/NoteTopicWithLink.vue"

const { managedApi } = useLoadingApi()

const notebooks = ref<BazaarNotebook[] | undefined>(undefined)

const fetchData = async () => {
  notebooks.value = await managedApi.restBazaarController.bazaar()
  notebooks.value = undefined
}

onMounted(() => {
  fetchData()
})
</script>
