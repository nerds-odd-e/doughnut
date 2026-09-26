<template>
  <ContentLoader v-if="attachmentRealm === undefined" />
  <div v-else class="container mx-auto pt-0 pb-4 max-w-6xl">
    <div class="attachment-page-summary" data-testid="attachment-page">
      <p class="text-sm text-base-content/70 mb-2">File</p>
      <h1 class="text-2xl mb-2" data-testid="attachment-page-filename">
        {{ attachmentRealm.attachment.filename }}
      </h1>
      <p class="mb-4" data-testid="attachment-page-size">
        {{ attachmentRealm.size }} bytes
      </p>
      <a
        class="daisy-btn daisy-btn-sm"
        data-testid="attachment-download-link"
        :href="downloadHref"
        :download="attachmentRealm.attachment.filename"
      >
        <Download class="size-4" aria-hidden="true" />
        Download
      </a>
      <button
        v-if="!attachmentRealm.notebookRealm.readonly"
        class="daisy-btn daisy-btn-sm daisy-btn-error daisy-btn-outline ml-2"
        data-testid="attachment-delete-button"
        @click="deleteAttachment"
      >
        <Trash2 class="size-4" aria-hidden="true" />
        Delete
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {
  DownloadAttachmentData,
  NotebookAttachmentRealm,
} from "@generated/donut-backend-api"
import { client } from "@generated/donut-backend-api/client.gen"
import { NotebookAttachmentController } from "@generated/donut-backend-api/sdk.gen"
import { Download, Trash2 } from "@lucide/vue"
import { computed } from "vue"
import { useRouter } from "vue-router"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { containingLocationOf } from "@/routes/containingLocation"

const props = defineProps<{
  attachmentRealm: NotebookAttachmentRealm | undefined
}>()

const downloadHref = computed(() =>
  client.buildUrl<DownloadAttachmentData>({
    url: "/api/notebooks/{notebook}/attachments/{attachment}/content",
    path: {
      notebook: props.attachmentRealm!.notebookRealm.notebook.id,
      attachment: props.attachmentRealm!.attachment.id,
    },
  })
)

const router = useRouter()
const { popups } = usePopups()

const deleteAttachment = async () => {
  const realm = props.attachmentRealm!
  if (!(await popups.confirm(`Delete ${realm.attachment.filename}?`))) return
  const { error } = await apiCallWithLoading(() =>
    NotebookAttachmentController.deleteAttachment({
      path: {
        notebook: realm.notebookRealm.notebook.id,
        attachment: realm.attachment.id,
      },
    })
  )
  if (error) return
  refreshSidebarStructuralListings()
  await router.push(containingLocationOf(realm))
}
</script>

<style scoped>
.attachment-page-summary {
  background: color-mix(in oklch, var(--color-base-200) 80%, transparent);
  border-radius: 8px;
  padding: 1.5rem;
}
</style>
