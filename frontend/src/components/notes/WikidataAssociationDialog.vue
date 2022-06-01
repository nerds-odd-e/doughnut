<template>
  <h3>
    Associate <strong>{{ note.title }}</strong> to Wikidata
  </h3>
  <form v-if="!showConfirmation" @submit.prevent.once="validateAssociation">
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

  <form v-else @submit.prevent.once="saveWiki">
    <p>
      Confirm to associate <strong>{{ note.title }}</strong> with
      <strong>{{ wikiDataTitle }}</strong
      >?
    </p>

    <input
      type="cancel"
      value="Cancel"
      class="btn btn-secondary"
      @click="showConfirmation = false"
    />

    <input type="submit" value="Confirm" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import TextInput from "../form/TextInput.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import SearchWikidata from "../search/SearchWikidata.vue";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: true, hasFormError: false });
  },
  props: { note: { type: Object as PropType<Generated.Note>, required: true } },
  components: { TextInput, SearchWikidata },
  emits: ["done"],
  data() {
    return {
      associationData: {
        wikidataId: "",
      } as Generated.WikidataAssociationCreation,
      wikiDataTitle: "",
      showConfirmation: false,
      wikidataIdError: undefined as undefined | string,
    };
  },
  methods: {
    async validateAssociation() {
      try {
        const res = await this.api.wikidata.getWikiData(
          this.associationData.wikidataId
        );
        if (res.WikiDataTitleInEnglish !== this.note.title) {
          this.wikiDataTitle = res.WikiDataTitleInEnglish;
          this.showConfirmation = true;
        } else {
          this.saveWiki();
        }
      } catch (e) {
        this.wikidataIdError = "The wikidata service is not available";
      }
    },
    saveWiki() {
      this.api.wikidata
        .updateWikidataId(this.note.id, this.associationData)
        .then(() => {
          this.$emit("done");
        });
    },
    populateWikidataId(wikidataId) {
      this.associationData.wikidataId = wikidataId;
    },
  },
});
</script>
