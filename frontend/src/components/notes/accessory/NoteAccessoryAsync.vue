<template>
  <div class="text-break">
    <NoteAccessoryDisplay
      v-if="noteAccessory"
      :note-accessory="noteAccessory"
    />
  </div>
</template>

<script lang="ts">
import { NoteAccessory } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { PropType, defineComponent } from "vue"
import NoteAccessoryDisplay from "./NoteAccessoryDisplay.vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
  props: {
    noteId: { type: Number, required: true },
    readonly: { type: Boolean, required: true },
    updatedNoteAccessory: {
      type: Object as PropType<NoteAccessory>,
    },
  },
  components: {
    NoteAccessoryDisplay,
  },
  data() {
    return {
      noteAccessory: undefined as NoteAccessory | undefined,
    }
  },
  watch: {
    updatedNoteAccessory() {
      this.noteAccessory = this.updatedNoteAccessory
    },
  },
  methods: {
    async fetchData() {
      this.noteAccessory =
        await this.managedApi.restNoteController.showNoteAccessory(this.noteId)
    },
  },
  mounted() {
    this.fetchData()
  },
})
</script>
