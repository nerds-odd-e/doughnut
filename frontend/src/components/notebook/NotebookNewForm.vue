<template>
  <form @submit.prevent.once="processForm">
    <p
      v-if="notebookGroup"
      class="mb-3 text-sm text-base-content/70"
      data-testid="notebook-new-form-group-hint"
    >
      Creates in group "{{ notebookGroup.name }}".
    </p>
    <PathNameEditor
      v-model="noteFormData.newTitle"
      :error-message="errors.newTitle"
      autofocus
      editor-role="textbox"
      placeholder="Notebook name"
    />
    <TextInput
      scope-name="notebook"
      field="description"
      v-model="noteFormData.description"
      :error-message="errors.description"
      placeholder="Optional short plain-text message (shown on notebook cards)"
    />
    <input type="submit" value="Submit" class="daisy-btn daisy-btn-primary" />
  </form>
</template>

<script lang="ts">
import PathNameEditor from "@/components/notes/core/PathNameEditor.vue"
import TextInput from "@/components/form/TextInput.vue"
import type {
  Circle,
  NotebookCreationRequest,
} from "@generated/doughnut-backend-api"
import {
  CircleController,
  NotebookController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { PropType } from "vue"

export default {
  props: {
    circle: { type: Object as PropType<Circle> },
    notebookGroup: {
      type: Object as PropType<{ id: number; name: string }>,
    },
  },
  components: {
    PathNameEditor,
    TextInput,
  },
  data() {
    return {
      noteFormData: {
        newTitle: "",
        description: "",
      },
      errors: {
        newTitle: undefined as undefined | string,
        description: undefined as undefined | string,
      } as Record<string, string | undefined>,
    }
  },
  methods: {
    creationBody(): NotebookCreationRequest {
      return {
        ...this.noteFormData,
        ...(this.notebookGroup
          ? { notebookGroupId: this.notebookGroup.id }
          : {}),
      }
    },
    async processForm() {
      const body = this.creationBody()
      const { data: result, error } = await apiCallWithLoading(() =>
        this.circle
          ? CircleController.createNotebookInCircle({
              path: { circle: this.circle.id },
              body,
            })
          : NotebookController.createNotebook({
              body,
            })
      )
      if (!error) {
        await this.$router.push({
          name: "notebookPage",
          params: { notebookId: String(result!.notebook.id) },
        })
      } else {
        // Error is handled by global interceptor (toast notification)
        // Extract field-level errors if available (for 400 validation errors)
        const errorObj = toOpenApiError(error)
        this.errors = { ...this.errors, ...(errorObj.errors || {}) }
      }
    },
  },
}
</script>
