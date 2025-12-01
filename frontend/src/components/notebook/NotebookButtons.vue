<template>
  <div class="daisy-btn-group daisy-btn-group-sm daisy-flex daisy-align-items-center daisy-flex-wrap">
    <BazaarNotebookButtons v-if="notebook.circle" :notebook="notebook" :logged-in="true" />
    <button
      class="daisy-btn daisy-btn-ghost daisy-btn-sm"
      @click="editNotebookSettings"
      title="Edit notebook settings"
    >
      <SvgEditNotebook />
    </button>
    <PopButton
      title="Move to ..."
      v-if="user?.externalIdentifier === notebook.creatorId"
    >
      <template #button_face>
        <SvgMoveToCircle />
      </template>
      <NotebookMoveDialog v-bind="{ notebook }" />
    </PopButton>
    <button
      class="daisy-btn daisy-btn-ghost daisy-btn-sm"
      @click="shareNotebook()"
      title="Share notebook to bazaar"
    >
      <SvgBazaarShare />
    </button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import SvgBazaarShare from "@/components/svgs/SvgBazaarShare.vue"
import SvgEditNotebook from "@/components/svgs/SvgEditNotebook.vue"
import SvgMoveToCircle from "@/components/svgs/SvgMoveToCircle.vue"
import type { Notebook, User } from "@generated/backend"
import { NotebookController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import NotebookMoveDialog from "./NotebookMoveDialog.vue"
import BazaarNotebookButtons from "@/components/bazaar/BazaarNotebookButtons.vue"

const router = useRouter()
const { popups } = usePopups()

const props = defineProps<{
  notebook: Notebook
  user?: User
}>()

const emit = defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
}>()

const editNotebookSettings = () => {
  router.push({
    name: "notebookEdit",
    params: { notebookId: props.notebook.id },
  })
}

const shareNotebook = async () => {
  if (await popups.confirm(`Confirm to share?`)) {
    const { error } = await apiCallWithLoading(() =>
      NotebookController.shareNotebook({
        path: { notebook: props.notebook.id },
      })
    )
    if (!error) {
      await router.push({ name: "notebooks" })
    }
  }
}
</script>
