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
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";

export default ({
  setup() {
    return useStoredLoadingApi({hasFormError: true});
  },
  components: { ContainerPage, TextInput },
  data() {
    return {
      formData: {},
    }
  },
  methods: {
    processForm() {
      this.storedApi().createUser(this.formData)
    },
  },
})
</script>
