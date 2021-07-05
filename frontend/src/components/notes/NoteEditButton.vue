<template>
  <ModalWithButton v-model="show">
      <template #button>
        <button class="btn btn-small" title="edit note" @click="show=true">
          <SvgEdit/>
        </button>
      </template>
      <template #header>
        <h3>{{note.title}}</h3>
      </template>
      <template #body>
        <form @submit.prevent="processForm">
            <NoteFormBody v-model="formData" :errors="formErrors"/>
            <input type="submit" value="Submit" class="btn btn-primary"/>
        </form>
      </template>
  </ModalWithButton>

</template>

<script>
import NoteBreadcrumbForOwnOrCircle from "./NoteBreadcrumbForOwnOrCircle.vue"
import NoteFormBody from "./NoteFormBody.vue"
import ModalWithButton from "../commons/ModalWithButton.vue"
import SvgEdit from "../svgs/SvgEdit.vue"
import { restPostMultiplePartForm } from "../../restful/restful"

export default {
  name: 'NoteNewButton',
  components: { NoteBreadcrumbForOwnOrCircle, NoteFormBody, ModalWithButton, SvgEdit },
  props: {note: Object},
  emits: ['updated'],
  data() {
    return {
      show: false,
      formData: {...this.note},
      formErrors: {},
      loading: false
    }
  },
  watch: {
    show() {
      Object.assign(this.formData, this.note)
      this.formErrors = {}
      this.loading = false
    }
  },
  methods: {
    processForm() {
      restPostMultiplePartForm(
        `/api/notes/${this.note.id}`,
        this.formData,
        r=>this.loading=r,
        (res) => {
          this.$emit('updated')
          this.show = false;
          this.$router.push({name: "noteShow", params: { noteid: res.noteId}})
        },
        (res) => this.formErrors = res
      )
    }

  },
}
</script>
