<template>
  <table class="table" v-if="approvals?.length">
    <thead>
    <tr>
      <th>Notebook</th>
      <th>Username</th>
    </tr>
    </thead>
    <tbody>
    <tr v-for="approval in approvals" :key="approval.id">
      <td>
        <NoteTopicWithLink v-bind="{ noteTopic: approval.notebook.headNote.noteTopic }" />
      </td>
      <td>
        {{ approval.notebook.creatorId }}
      </td>
      <td>
        <button
          class="btn btn-primary"
          style="background-color: green; border-color: green;"
          @click="approveNoteBook(approval.notebook.id)"
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
import { NotebookCertificateApproval } from "@/generated/backend"
import NoteTopicWithLink from "@/components/notes/NoteTopicWithLink.vue"
import usePopups from "../commons/Popups/usePopups"

const { popups } = usePopups()
const { managedApi } = useLoadingApi()

const approvals = ref<NotebookCertificateApproval[] | undefined>(undefined)

const fetchNotebooks = async () => {
  approvals.value =
    await managedApi.restNotebookCertificateApprovalController.getAllPendingRequestNotebooks()
}

const approveNoteBook = async (notebookId: number) => {
  if (await popups.confirm(`Are you sure you want to approve this notebook?`)) {
    await managedApi.restNotebookCertificateApprovalController.approveNoteBook(
      notebookId
    )
    fetchNotebooks()
  }
}
onMounted(() => {
  fetchNotebooks()
})
</script>
