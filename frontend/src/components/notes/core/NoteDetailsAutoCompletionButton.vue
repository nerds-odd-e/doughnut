<template>
  <a
    :title="'auto-complete details'"
    class="btn btn-sm"
    role="button"
    @click="initialAutoCompleteDetails"
  >
    <SvgRobot />
  </a>
</template>

<script lang="ts">
import type { AiAssistantResponse, Note } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import SvgRobot from "../../svgs/SvgRobot.vue"

export default defineComponent({
  setup() {
    return {
      ...useLoadingApi(),
    }
  },
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    note: {
      type: Object as PropType<Note>,
      required: true,
    },
  },
  components: {
    SvgRobot,
  },
  data() {
    return {
      isUnmounted: false,
      threadRespons: undefined as undefined | AiAssistantResponse,
    }
  },
  methods: {
    async initialAutoCompleteDetails() {
      const response = await this.managedApi.restAiController.getCompletion(
        this.note.id,
        {
          detailsToComplete: this.note.details,
        }
      )
      return this.autoCompleteDetails(response)
    },
    async autoCompleteDetails(response: AiAssistantResponse) {
      if (this.isUnmounted) return

      this.storageAccessor
        .storedApi()
        .appendDetails(this.note.id, response.requiredAction!.contentToAppend!)
    },
  },
  unmounted() {
    this.isUnmounted = true
  },
})
</script>
