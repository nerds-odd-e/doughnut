<template>
  <ModalWithButton v-model="show">
      <template #button>
        <button class="btn btn-sm" role="button" @click="show=true" title="Add to my learning">
            <SvgAdd/>
        </button>
      </template>
      <template #header>
        <h3>Add to my learning</h3>
      </template>
      <template #body>
        <p v-if="!user">Please login first</p>
        <form v-else @submit.prevent.once="processForm">
            <TextInput scopeName='subscription' field='dailyTargetOfNewNotes' v-model="formData.dailyTargetOfNewNotes" :errors="formErrors.dailyTargetOfNewNotes"/>
            <input type="submit" value="Submit" class="btn btn-primary"/>
        </form>
      </template>
  </ModalWithButton>


</template>

<script>
import SvgAdd from "../svgs/SvgAdd.vue"
import ModalWithButton from "../commons/ModalWithButton.vue"
import TextInput from "../form/TextInput.vue"
import { restPostMultiplePartForm } from "../../restful/restful"

export default {
  props: {notebook: Object, user: Object},
  components: { SvgAdd, TextInput, ModalWithButton },
  data(){
    return {
      show: false, 
      formData: {dailyTargetOfNewNotes: 5},
      formErrors: {}
    }
  },

  methods: {
  processForm() {
    restPostMultiplePartForm( `/api/subscriptions/notebooks/${this.notebook.id}/subscribe`, this.formData, r=>this.loading=r)
          .then(res => {
            this.show = false;
            this.$router.push({name: "notebooks"})
          })
          .catch(res => this.formErrors = res)
      }
  }
 
}
</script>
