<template>
  <ModalWithButton v-model="show">
      <template #button>
        <slot :open="()=>show=true"/>
      </template>
      <template #header>
        <NoteBreadcrumbForOwnOrCircle v-bind="{notebook, ancestors}">
            <li class="breadcrumb-item">(adding here)</li>
        </NoteBreadcrumbForOwnOrCircle>
      </template>
      <template #body>
        <form @submit.prevent="processForm">
            <NoteFormBody v-model="noteFormData" :errors="noteFormErrors"/>
            <input type="submit" value="Submit" class="btn btn-primary"/>
        </form>
      </template>
  </ModalWithButton>

</template>

<script>
import NoteBreadcrumbForOwnOrCircle from "./NoteBreadcrumbForOwnOrCircle.vue"
import NoteFormBody from "./NoteFormBody.vue"
import ModalWithButton from "../commons/ModalWithButton.vue"
import { restPostMultiplePartForm } from "../../restful/restful"

export default {
  name: 'NoteNewButton',
  components: {NoteBreadcrumbForOwnOrCircle, NoteFormBody, ModalWithButton},
  props: {notebook: Object, ancestors: Array},
  data() {
    return {
      show: false,
      noteFormErrors: {},
      noteFormData: {}
    }
  },
  methods: {
    processForm() {
      restPostMultiplePartForm(
        `/api/notes/${this.ancestors[this.ancestors.length - 1].id}/create`,
        this.noteFormData,
        r=>this.loading=r,
        (res) => {
          this.show = false;
          this.$router.push({name: "noteShow", params: { noteid: res.noteId}})
        },
        (res) => this.noteFormErrors = res
      )
    }
  },
}
</script>
