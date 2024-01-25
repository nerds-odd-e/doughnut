<template>
  <h3>
    Associate <strong>{{ note.topicConstructor }}</strong> to Wikidata
  </h3>
  <form v-if="!conflictWikidataTitle" @submit.prevent="validateAndSave">
    <TextInput
      scope-name="wikidataID"
      field="wikidataID"
      v-model="associationData.wikidataId"
      :errors="wikidataIdError"
      placeholder="example: `Q12345`"
      v-focus
    />

    <input type="submit" value="Save" class="btn btn-primary" />
  </form>

  <form v-else @submit.prevent="save">
    <p>
      Confirm to associate <strong>{{ note.topicConstructor }}</strong> with
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
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["closeDialog"],
  components: { TextInput },
  data() {
    return {
      associationData: {
        wikidataId: this.note.wikidataId,
      } as Generated.WikidataAssociationCreation,
      conflictWikidataTitle: undefined as undefined | string,
      wikidataIdError: undefined as undefined | string,
    };
  },
  methods: {
    async validateAndSave() {
      try {
        const res = await this.api.wikidata.getWikidataEntityById(
          this.associationData.wikidataId,
        );
        if (
          res.WikidataTitleInEnglish !== "" &&
          res.WikidataTitleInEnglish.toUpperCase() !==
            this.note.topicConstructor.toUpperCase()
        ) {
          this.conflictWikidataTitle = res.WikidataTitleInEnglish;
          return;
        }
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      } catch (e: any) {
        this.wikidataIdError = e.message;
      }
      await this.save();
    },
    async save() {
      try {
        await this.storageAccessor
          .storedApi()
          .updateWikidataId(this.note.id, this.associationData);
        this.$emit("closeDialog");
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      } catch (e: any) {
        this.wikidataIdError = e.wikidataId;
      }
    },
  },
});
</script>
