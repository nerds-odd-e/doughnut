<template>
      <h3>Creating a new circle</h3>
      <form @submit.prevent.once="processForm">
        <TextInput
          scopeName="circle"
          field="name"
          v-model="formData.name"
          :errors="formErrors.name"
        />
        <input type="submit" value="Submit" class="btn btn-primary" />
      </form>
</template>

<script>
import TextInput from "../form/TextInput.vue";
import api from  "../../managedApi/api";

export default {
  props: { notebook: Object, user: Object },
  components: { TextInput },
  data() {
    return {
      formData: {},
      formErrors: {},
    };
  },

  methods: {
    processForm() {
      this.loading = true
      api(this).circleMethods.createCircle(this.formData)
        .then((res) => {
          this.$router.push({
            name: "circleShow",
            params: { circleId: res.id },
          });
        })
        .catch((res) => (this.formErrors = res))
        .finally(()=>this.loading = false);
    },
  },
};
</script>
