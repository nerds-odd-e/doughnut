<template>
  <form @submit.prevent.once="processForm">
    <NoteFormBody v-if="!!formData" v-model="formData" :errors="formErrors" />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import Doughnut from "../../@types/Doughnut/index.d"
import { defineComponent } from "vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import NoteFormBody from "./NoteFormBody.vue";

export default defineComponent({
  setup() {
    return useStoredLoadingApi({initalLoading: true, hasFormError: true});
  },
  name: "NoteEditDialog",
  components: {
    NoteFormBody,
  },
  props: { noteId: {type: Doughnut.IDPropType, required: true}},
  emits: ["done"],
  data() {
    return {
      formData: null,
    } as {
      formData: any
    };
  },

  methods: {
    fetchData() {
      this.storedApi.getNoteAndItsChildren(this.noteId)
      .then((res) => {
          const { updatedAt, ...rest } = res.notes[0].note.noteAccessories
          this.formData = rest
        }
      )
    },

    processForm() {
      this.storedApi.updateNote(this.noteId, this.formData)
      .then(() => {
        this.$emit("done");
      })
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
