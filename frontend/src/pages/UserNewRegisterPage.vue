<template>
    <ContainerPage v-bind="{ loading, contentExists: true, title: 'Welcome, new user. Please create your profile' }">
      <form @submit.prevent.once="processForm">
        <TextInput
          scopeName="user"
          field="name"
          v-model="formData.name"
          :autofocus="true"
          :errors="formErrors.name"
          :placeholder="Nickname"
        />
        <input type="submit" value="Submit" class="btn btn-primary" />
      </form>
    </ContainerPage>
</template>

<script>
import ContainerPage from "./commons/ContainerPage.vue";
import TextInput from "../components/form/TextInput.vue";
import storedComponent from "../store/storedComponent";

export default storedComponent({
  components: { ContainerPage, TextInput },
  data() {
    return {
      loading: false,
      formData: {},
      formErrors: {},
    }
  },
  methods: {
    processForm() {
      this.storedApiExp().createUser(this.formData)
        .catch((res) => this.formErrors.value = res)
    },
  },
})
</script>
