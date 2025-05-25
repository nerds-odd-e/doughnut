<template>
  <div class="daisy-btn-group daisy-btn-group-sm daisy-flex daisy-align-items-center daisy-flex-wrap">
    <BazaarNotebookButtons v-if="notebook.circle" :notebook="notebook" :logged-in="true" />
    <PopButton title="Edit notebook settings">
      <template #button_face>
        <SvgEditNotebook />
      </template>
      <NotebookEditDialog v-bind="{ notebook, user }" />
    </PopButton>
    <PopButton
      title="Move to ..."
      v-if="user?.externalIdentifier === notebook.creatorId"
    >
      <template #button_face>
        <SvgMoveToCircle />
      </template>
      <NotebookMoveDialog v-bind="{ notebook }" />
    </PopButton>
    <PopButton
      title="Notebook Questions"
      v-if="user?.externalIdentifier === notebook.creatorId"
    >
      <template #button_face>
        <SvgRaiseHand />
      </template>
      <NotebookQuestionsDialog v-bind="{ notebook }" />
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
import SvgRaiseHand from "@/components/svgs/SvgRaiseHand.vue"
import type { Notebook, User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NotebookEditDialog from "./NotebookEditDialog.vue"
import NotebookMoveDialog from "./NotebookMoveDialog.vue"
import NotebookQuestionsDialog from "./NotebookQuestionsDialog.vue"
import BazaarNotebookButtons from "@/components/bazaar/BazaarNotebookButtons.vue"

const { managedApi } = useLoadingApi()
const router = useRouter()
const { popups } = usePopups()

const props = defineProps<{
  notebook: Notebook
  user?: User
}>()

const shareNotebook = async () => {
  if (await popups.confirm(`Confirm to share?`)) {
    await managedApi.restNotebookController.shareNotebook(props.notebook.id)
    router.push({ name: "notebooks" })
  }
}
</script>
