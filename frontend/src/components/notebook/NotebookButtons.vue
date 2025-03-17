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
    <label
      class="daisy-btn daisy-btn-ghost daisy-btn-sm"
      title="Import from Obsidian"
    >
      <input
        type="file"
        accept=".zip"
        class="!hidden"
        style="display: none !important"
        @change="handleObsidianImport"
      />
      <SvgObsidian />
    </label>
    <button
      class="daisy-btn daisy-btn-ghost daisy-btn-sm"
      @click="exportForObsidian"
      title="Export notebook for Obsidian"
    >
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 256 256" class="fill-current">
        <path d="M128 20.2L28.8 86.4v83.2L128 235.8l99.2-66.2V86.4L128 20.2zm0 23.8l71.8 48L128 140.2 56.2 92l71.8-48zm-85.2 55l78.2 52.3v77.5l-78.2-52.3v-77.5zm170.4 0v77.5l-78.2 52.3v-77.5l78.2-52.3z"/>
      </svg>
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
import SvgObsidian from "@/components/svgs/SvgObsidian.vue"
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

const exportForObsidian = () => {
  const link = document.createElement("a")
  link.style.display = "none"
  link.href = `/api/notebooks/${props.notebook.id}/obsidian`
  link.download = `${props.notebook.title}-obsidian.zip`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const handleObsidianImport = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  try {
    await managedApi.restNotebookController.importObsidian(props.notebook.id, {
      file,
    })
    // Clear file input for reuse
    ;(event.target as HTMLInputElement).value = ""
  } catch (error) {
    alert("Failed to import file")
    console.error("Import error:", error)
  }
}
</script>
