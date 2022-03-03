<template>
  <form @submit.prevent.once="processForm">
    <NoteFormBody v-if="!!formData" v-model="formData" :errors="formErrors" />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script>
import NoteFormBody from "./NoteFormBody.vue";
import storedApi from  "../../managedApi/storedApi";

export default {
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
      this.loading = true
      storedApi(this.$store).getNoteAndItsChildren(this.noteId)
      .then((res) => {
          const { updatedAt, ...rest } = res.notes[0].noteAccessories
          this.formData = rest
        }
      )
      .finally(() => this.loading = false)
    },

    processForm() {
      this.loading = true
      storedApi(this.$store).updateNote(this.noteId, this.formData)
      .then(() => {
        this.$emit("done");
      })
      .catch((res) => (this.formErrors = res))
      .finally(() => this.loading = false)
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
