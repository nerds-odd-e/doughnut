<template>
  <NoteInfoComponent
    v-if="noteInfo?.note"
    :note-info="noteInfo"
    @level-changed="$emit('levelChanged', $event)"
  />
</template>

<script lang="ts">
import type { NoteInfo } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { defineComponent } from "vue"
import NoteInfoComponent from "./NoteInfoComponent.vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
  props: { noteId: { type: Number, required: true } },
  emits: ["levelChanged"],
  components: { NoteInfoComponent },
  data() {
    return { noteInfo: undefined as undefined | NoteInfo }
  },
  methods: {
    fetchData() {
      this.managedApi.restNoteController
        .getNoteInfo(this.noteId)
        .then((articles) => {
          this.noteInfo = articles
        })
    },
  },
  mounted() {
    this.fetchData()
  },
})
</script>
