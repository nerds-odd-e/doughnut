<template>
      <h3>Link</h3>
      <div v-if="!!inverseIcon">
        Source:
        <strong>
          <NoteTitleWithLink
            class="link-title"
            v-bind="{ note: link.sourceNote }"
          />
        </strong>
      </div>
      <LinkTypeSelect
        field="linkType"
        scopeName="link"
        v-model="formData.typeId"
        :errors="formErrors.typeId"
        :inverseIcon="true"
      />
      <div v-if="!inverseIcon">
        Target:
        <strong>
          <NoteTitleWithLink
            class="link-title"
            v-bind="{ note: link.targetNote }"
          />
        </strong>
      </div>

      <button class="btn btn-primary" v-on:click="updateLink()">Update</button>
      <button class="btn btn-danger" v-on:click="deleteLink()">Delete</button>
</template>

<script>
import LinkTypeSelect from "./LinkTypeSelect.vue";
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";

export default ({
  setup() {
    return useStoredLoadingApi({hasFormError: true});
  },
  props: { link: Object, inverseIcon: Boolean, colors: Object },
  emits: ["done"],
  components: {
    LinkTypeSelect,
    NoteTitleWithLink,
  },
  data() {
    return {
      formData: { typeId: this.link.typeId },
    };
  },

  methods: {
    updateLink() {
      this.storedApi.updateLink(this.link.id, this.formData)
        .then((res) => this.$emit('done'))
    },

    async deleteLink() {
      if (!(await this.$popups.confirm("Are you sure to delete this link?")))
        return;
      this.storedApi.deleteLink(this.link.id)
        .then((res) => { this.$emit('done') })
    },
  },
});
</script>

<style scoped>
.link-nob {
  padding: 3px;
}
</style>
