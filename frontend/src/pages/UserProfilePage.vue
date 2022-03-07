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
import storedComponent from "../store/storedComponent";
import useLoadingApi from "../managedApi/useLoadingApi";

export default storedComponent({
  setup() {
    return useLoadingApi({initalLoading: true, hasFormError: true})
  },
  components: { ContainerPage, TextInput },
  emits: ["userUpdated"],
  data() {
    return {
      formData: null,
    };
  },
  methods: {
    fetchData() {
      this.apiExp().userMethods.currentUser().then((res) => {
        this.formData = res
      })
    },
    processForm() {
      this.storedApi().updateUser(this.formData.id, this.formData)
        .then(() =>  this.$router.push({ name: "root" }))

    },
  },

  mounted() {
    this.fetchData();
  },
});
</script>
