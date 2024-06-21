<template>
  <div class="btn-group btn-group-sm">
    <slot name="additional-buttons" />
    <PopButton title="Edit notebook settings">
      <template #button_face>
        <SvgEditNotebook />
      </template>
      <NotebookEditDialog v-bind="{ notebook }" />
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

<script lang="ts">
import PopButton from "@/components/commons/Popups/PopButton.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import SvgBazaarShare from "@/components/svgs/SvgBazaarShare.vue"
import SvgEditNotebook from "@/components/svgs/SvgEditNotebook.vue"
import { Notebook } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { PropType, defineComponent } from "vue"
import NotebookEditDialog from "./NotebookEditDialog.vue"

export default defineComponent({
  setup() {
    return { ...useLoadingApi(), ...usePopups() }
  },
  props: {
    notebook: { type: Object as PropType<Notebook>, required: true },
  },
  components: {
    SvgBazaarShare,
    PopButton,
    NotebookEditDialog,
    SvgEditNotebook,
  },
  methods: {
    async shareNotebook() {
      if (await this.popups.confirm(`Confirm to share?`)) {
        this.managedApi.restNotebookController
          .shareNotebook(this.notebook.id)
          .then(() => this.$router.push({ name: "notebooks" }))
      }
    },
  },
})
</script>
