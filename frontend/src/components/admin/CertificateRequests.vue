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
import { NotebookCertificateApprovalController } from "@generated/backend/sdk.gen"
import { globalClientSilent } from "@/managedApi/clientSetup"
import type { NotebookCertificateApproval } from "@generated/backend"
import NotebookLink from "@/components/notes/NotebookLink.vue"
import usePopups from "../commons/Popups/usePopups"

const { popups } = usePopups()

const approvals = ref<NotebookCertificateApproval[] | undefined>(undefined)

const fetchNotebooks = async () => {
  const { data: pendingRequests, error } =
    await NotebookCertificateApprovalController.getAllPendingRequest({
      client: globalClientSilent,
    })
  if (!error) {
    approvals.value = pendingRequests!
  }
}

const approveNoteBook = async (approvalId: number) => {
  if (await popups.confirm(`Are you sure you want to approve this notebook?`)) {
    const { error } = await NotebookCertificateApprovalController.approve({
      path: { notebookCertificateApproval: approvalId },
    })
    if (!error) {
      fetchNotebooks()
    }
  }
}
onMounted(() => {
  fetchNotebooks()
})
</script>
