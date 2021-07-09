<template>
  <ModalWithButton v-model="show">
      <template #button>
        <button class="dropdown-item" title="edit note" @click="show=true">
          <SvgReviewSetting/>
          <slot />
        </button>
      </template>
      <template #header>
        <h3>Edit review setting for <em>{{oldTitle}}</em></h3>
      </template>
      <template #body>
        <form @submit.prevent="processForm">
            <CheckInput scopeName='review-setting' field='rememberSpelling' v-model="formData.rememberSpelling" :errors="formErrors.rememberSpelling"/>
            <input type="submit" value="Submit" class="btn btn-primary"/>
        </form>
      </template>
  </ModalWithButton>

</template>

<script>
import NoteBreadcrumbForOwnOrCircle from "../notes/NoteBreadcrumbForOwnOrCircle.vue"
import NoteFormBody from "../notes/NoteFormBody.vue"
import ModalWithButton from "../commons/ModalWithButton.vue"
import SvgReviewSetting from "../svgs/SvgReviewSetting.vue"
import CheckInput from "../form/CheckInput.vue"
import { restGet, restPost } from "../../restful/restful"

export default {
  components: { CheckInput, NoteBreadcrumbForOwnOrCircle, NoteFormBody, ModalWithButton, SvgReviewSetting },
  props: {noteId: Number, oldTitle: String},
  emits: ['updated'],
  data() {
    return {
      show: false,
      formData: null,
      formErrors: {},
      loading: false
    }
  },
  watch: {
    show() {
      if(this.show) {
        this.formErrors = {}
        this.formData = null
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData() {
      restGet(`/api/notes/${this.noteId}/review-setting`,
        (r)=>this.loading=r,
        (res) => this.formData = res)
    },
    processForm() {
      restPost(
        `/api/notes/${this.noteId}/review-setting`,
        this.formData,
        r=>this.loading=r,
        (res) => {
          this.$emit('updated')
          this.show = false
        },
        (res) => this.formErrors = res
      )
    }

  },
}
</script>
