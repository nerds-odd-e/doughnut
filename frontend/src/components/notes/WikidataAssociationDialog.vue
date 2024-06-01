<template>
  <h3>
    Associate
    <strong
      ><NoteTopicComponent v-bind="{ noteTopic: note.noteTopic }"
    /></strong>
    Wikidata
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
      Confirm to associate
      <strong>NoteTopic v-bind="{ noteTopic: note.noteTopic }" /></strong> with
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

<script setup lang="ts">
import { PropType, ref } from "vue";
import { Note, WikidataAssociationCreation } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import TextInput from "../form/TextInput.vue";
import NoteTopicComponent from "./core/NoteTopicComponent.vue";

const { managedApi } = useLoadingApi();
const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const emit = defineEmits(["closeDialog"]);

const associationData = ref<WikidataAssociationCreation>({
  wikidataId: props.note.wikidataId!,
});
const conflictWikidataTitle = ref<string | undefined>(undefined);
const wikidataIdError = ref<string | undefined>(undefined);

const save = async () => {
  try {
    await props.storageAccessor
      .storedApi()
      .updateWikidataId(props.note.id, associationData.value);
    emit("closeDialog");
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (e: any) {
    wikidataIdError.value = e.wikidataId;
  }
};

const validateAndSave = async () => {
  try {
    const res =
      await managedApi.restWikidataController.fetchWikidataEntityDataById(
        associationData.value.wikidataId,
      );
    if (
      res.WikidataTitleInEnglish !== "" &&
      res.WikidataTitleInEnglish.toUpperCase() !==
        props.note.noteTopic.topicConstructor.toUpperCase()
    ) {
      conflictWikidataTitle.value = res.WikidataTitleInEnglish;
      return;
    }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } catch (e: any) {
    wikidataIdError.value = e.body.message;
  }
  await save();
};
</script>
