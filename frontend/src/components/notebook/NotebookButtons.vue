<template>
  <div class="btn-group btn-group-sm">
    <slot name="additional-buttons" />
    <PopButton title="Edit notebook settings">
      <template #button_face>
        <SvgEditNotebook />
      </template>
      <NotebookEditDialog v-bind="{ notebook }" />
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
    <PopButton title="Notebook Assistant" v-if="user?.admin">
      <template #button_face>
        <SvgRobot />
      </template>
      <template #default="{ closer }">
        <NotebookAssistantManagementDialog
          v-bind="{ notebook }"
          @close="closer($event)"
        />
      </template>
    </PopButton>
    <button
      class="btn btn-sm"
      title="Share notebook to bazaar"
      @click="shareNotebook()"
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
import { Notebook, User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { PropType } from "vue"
import NotebookEditDialog from "./NotebookEditDialog.vue"
import NotebookMoveDialog from "./NotebookMoveDialog.vue"
import NotebookQuestionsDialog from "./NotebookQuestionsDialog.vue"
import NotebookAssistantManagementDialog from "./NotebookAssistantManagementDialog.vue"
import SvgRobot from "../svgs/SvgRobot.vue"

const { managedApi } = useLoadingApi()
const router = useRouter()
const { popups } = usePopups()

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  user: { type: Object as PropType<User>, required: false },
})
const shareNotebook = async () => {
  if (await popups.confirm(`Confirm to share?`)) {
    managedApi.restNotebookController
      .shareNotebook(props.notebook.id)
      .then(() => router.push({ name: "notebooks" }))
  }
}
</script>
