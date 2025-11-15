<template>
  <table class="daisy-table" v-if="approvals?.length">
    <thead>
    <tr>
      <th>Notebook</th>
      <th>Username</th>
    </tr>
    </thead>
    <tbody>
    <tr v-for="approval in approvals" :key="approval.id">
      <td>
        <NotebookLink :notebook="approval.notebook" />
      </td>
      <td>
        {{ approval.notebook.creatorId }}
      </td>
      <td>
        <button
          class="daisy-btn daisy-btn-primary"
          style="background-color: green; border-color: green;"
          @click="approveNoteBook(approval.id)"
        >
        Approve
        </button>
      </td>
    </tr>
    </tbody>
  </table>
  <div v-else>
    No certification request found.
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { NotebookCertificateApproval } from "@generated/backend"
import NotebookLink from "@/components/notes/NotebookLink.vue"
import usePopups from "../commons/Popups/usePopups"

const { popups } = usePopups()
const { managedApi } = useLoadingApi()

const approvals = ref<NotebookCertificateApproval[] | undefined>(undefined)

const fetchNotebooks = async () => {
  approvals.value =
    await managedApi.services.getAllPendingRequest()
}

const approveNoteBook = async (approvalId: number) => {
  if (await popups.confirm(`Are you sure you want to approve this notebook?`)) {
    await managedApi.services.approve({
      notebookCertificateApproval: approvalId,
    })
    fetchNotebooks()
  }
}
onMounted(() => {
  fetchNotebooks()
})
</script>
