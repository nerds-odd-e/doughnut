<template>
  <ContainerPage v-bind="{ loading, contentExists: !!formData, title: 'Edit User Setting' }">
    <div v-if="!!formData">
      <form @submit.prevent.once="processForm">
        <TextInput
          scopeName="user"
          field="name"
          v-model="formData.name"
          :autofocus="true"
          :errors="formErrors.name"
        />
        <TextInput
          scopeName="user"
          field="dailyNewNotesCount"
          v-model="formData.dailyNewNotesCount"
          :errors="formErrors.dailyNewNotesCount"
        />
        <TextInput
          scopeName="user"
          field="spaceIntervals"
          v-model="formData.spaceIntervals"
          :errors="formErrors.spaceIntervals"
        />
        <input type="submit" value="Submit" class="btn btn-primary" />
      </form>
    </div>
  </ContainerPage>
</template>

<script>
import ContainerPage from "./commons/ContainerPage.vue";
import TextInput from "../components/form/TextInput.vue";
import api from "../managedApi/api"
import storedComponent from "../store/storedComponent";

export default storedComponent({
  components: { ContainerPage, TextInput },
  emits: ["userUpdated"],
  data() {
    return {
      loading: true,
      formData: null,
      formErrors: {},
    };
  },
  methods: {
    fetchData() {
      api(this).userMethods.currentUser().then((res) => {
        this.formData = res
      })
    },
    processForm() {
      this.storedApiExp().updateUser(this.formData.id, this.formData)
        .then(() =>  this.$router.push({ name: "root" }))
        .catch((res) => (this.formErrors = res))

    },
  },

  mounted() {
    this.fetchData();
  },
});
</script>
