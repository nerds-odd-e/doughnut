<template>
  <h3>
    Associate <strong>{{ note.title }}</strong> to Wikidata
  </h3>
  <form v-if="!conflictWikidataTitle" @submit.prevent.once="validateAndSave">
    <TextInput
      scope-name="wikidataID"
      field="wikidataID"
      v-model="associationData.wikidataId"
      :errors="wikidataIdError"
      placeholder="example: `Q1234`"
      v-focus
    />

    <input type="submit" value="Save" class="btn btn-primary" />
  </form>

  <form v-else @submit.prevent.once="save">
    <p>
      Confirm to associate <strong>{{ note.title }}</strong> with
      <strong>{{ conflictWikidataTitle }}</strong
      >?
    </p>

    <input
      type="cancel"
      value="Cancel"
      class="btn btn-secondary"
      @click="conflictWikidataTitle = undefined"
    />

    <input type="submit" value="Confirm" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import TextInput from "../form/TextInput.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: true, hasFormError: false });
  },
  props: { note: { type: Object as PropType<Generated.Note>, required: true } },
  components: { TextInput },
  emits: ["done"],
  data() {
    return {
      associationData: {
        wikidataId: "",
      } as Generated.WikidataAssociationCreation,
      conflictWikidataTitle: undefined as undefined | string,
      wikidataIdError: undefined as undefined | string,
    };
  },
  methods: {
    async validateAndSave() {
      try {
        const res = await this.api.wikidata.getWikiData(
          this.associationData.wikidataId
        );
        if (res.WikiDataTitleInEnglish !== this.note.title) {
          this.conflictWikidataTitle = res.WikiDataTitleInEnglish;
          return;
        }
        this.save();
      } catch (e) {
        this.wikidataIdError = "The wikidata service is not available";
      }
    },
    async save() {
      await this.api.wikidata.updateWikidataId(
        this.note.id,
        this.associationData
      );
      this.$emit("done");
    },
  },
});
</script>
