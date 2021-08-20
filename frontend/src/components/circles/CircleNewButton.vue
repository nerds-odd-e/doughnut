<template>
  <ModalWithButton v-model="show">
      <template #button>
        <button class="btn btn-sm" role="button" @click="show=true" title="Create a new circle">
            Create a new circle
        </button>
      </template>
      <template #header>
        <h3>Add to my learning</h3>
      </template>
      <template #body>
        <form @submit.prevent.once="processForm">
            <TextInput scopeName='circle' field='name' v-model="formData.name" :errors="formErrors.name"/>
            <input type="submit" value="Submit" class="btn btn-primary"/>
        </form>
      </template>
  </ModalWithButton>


</template>

<script>
import ModalWithButton from "../commons/ModalWithButton.vue"
import TextInput from "../form/TextInput.vue"
import { restPostMultiplePartForm } from "../../restful/restful"

export default {
  props: {notebook: Object, user: Object},
  components: { TextInput, ModalWithButton },
  data(){
    return {
      show: false, 
      formData: {},
      formErrors: {}
    }
  },

  methods: {
  processForm() {
    restPostMultiplePartForm( `/api/circles`, this.formData, r=>this.loading=r)
          .then(res => {
            this.show = false;
            this.$router.push({name: "circleShow", params: {circleId: res.id}})
          })
          .catch(res => this.formErrors = res)
      }
  }
 
}
</script>
