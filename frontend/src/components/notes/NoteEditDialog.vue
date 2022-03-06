<template>
  <form @submit.prevent.once="processForm">
    <NoteFormBody v-if="!!formData" v-model="formData" :errors="formErrors" />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script>
import NoteFormBody from "./NoteFormBody.vue";
import storedApi from  "../../managedApi/storedApi";
import storedComponent from "../../store/storedComponent";

export default storedComponent({
  name: "NoteEditDialog",
  components: {
    NoteFormBody,
  },
  props: { noteId: [String, Number] },
  emits: ["done"],
  data() {
    return {
      formData: null,
      formErrors: {},
      loading: true,
    };
  },

  methods: {
    fetchData() {
      this.storedApiExp().getNoteAndItsChildren(this.noteId)
      .then((res) => {
          const { updatedAt, ...rest } = res.notes[0].noteAccessories
          this.formData = rest
        }
      )
    },

    processForm() {
      storedApi(this).updateNote(this.noteId, this.formData)
      .then(() => {
        this.$emit("done");
      })
      .catch((res) => (this.formErrors = res))
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
