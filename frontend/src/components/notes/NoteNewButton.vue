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
            <LinkTypeSelect scopeName='note' field='linkTypeToParent' :allowEmpty="true" v-model="creationData.linkTypeToParent" :errors="formErrors.linkTypeToParent"/>
            <NoteFormBody v-model="creationData.noteContent" :errors="formErrors.noteContent"/>
            <input type="submit" value="Submit" class="btn btn-primary"/>
        </form>
      </template>
  </ModalWithButton>

</template>

<script>
import NoteBreadcrumbForOwnOrCircle from "./NoteBreadcrumbForOwnOrCircle.vue"
import NoteFormBody from "./NoteFormBody.vue"
import ModalWithButton from "../commons/ModalWithButton.vue"
import LinkTypeSelect from "../links/LinkTypeSelect.vue"
import { restGet, restPostMultiplePartForm } from "../../restful/restful"

function initialState() {
  return {
    creationData: {
      linkTypeToParent: '',
      noteContent: {},
    },
    formErrors: {},
  }
}

export default {
  name: 'NoteNewButton',
  components: { NoteBreadcrumbForOwnOrCircle, NoteFormBody, ModalWithButton, LinkTypeSelect },
  props: {parentId: Number},
  data() {
    return {
      loading: false,
      ancestors: null,
      notebook: null,
      show: false,
      ...initialState(),
    }
  },
  watch: {
    show() {
      Object.assign(this.$data, initialState())
      if(this.show) this.fetchData()
    }
  },
  methods: {
    fetchData() {
      restGet(`/api/notes/${this.parentId}`, (r)=>this.loading=r)
        .then( res => {
          const { ancestors, note, notebook} = res
          this.ancestors = [...ancestors, note]
          this.notebook = notebook
        })
    }, 

    processForm() {
      restPostMultiplePartForm(
        `/api/notes/${this.parentId}/create`,
        this.creationData,
        r=>this.loading=r)
          .then(res => {
            this.show = false;
            this.$router.push({name: "noteShow", params: { noteId: res.noteId}})
          })
          .catch(res => this.formErrors = res)
    }
  },
}
</script>
