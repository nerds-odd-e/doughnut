<template>
<div class="daisy-card daisy-w-96">
  <div class="daisy-card-body">
    <h3 class="daisy-card-title">
      Associate
      <strong
        ><NoteTitleComponent v-bind="{ noteTopology: note.noteTopology }"
      /></strong>
      Wikidata
    </h3>
    <form v-if="!conflictWikidataTitle" @submit.prevent="validateAndSave">
      <TextInput
        scope-name="wikidataID"
        field="wikidataID"
        v-model="associationData.wikidataId"
        :error-message="wikidataIdError"
        placeholder="example: `Q12345`"
        v-focus
      />

      <input type="submit" value="Save" class="daisy-btn daisy-btn-primary daisy-mt-4" />
    </form>

    <form v-else @submit.prevent="save">
      <p class="daisy-py-4">
        Confirm to associate
        <strong>NoteTopology v-bind="{ noteTopology: note.noteTopology }" /></strong> with
        <strong>{{ conflictWikidataTitle }}</strong
        >?
      </p>
      <div class="daisy-flex daisy-gap-2 daisy-justify-end">
        <input
          type="cancel"
          value="Cancel"
          class="daisy-btn daisy-btn-ghost"
          @click="conflictWikidataTitle = undefined"
        />

        <input type="submit" value="Confirm" class="daisy-btn daisy-btn-primary" />
      </div>
    </form>
  </div>
</div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type { Note, WikidataAssociationCreation } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import TextInput from "../form/TextInput.vue"
import NoteTitleComponent from "./core/NoteTitleComponent.vue"

interface WikidataError {
  body: {
    message: string
  }
}

interface WikidataIdError {
  wikidataId: string
}

const { managedApi } = useLoadingApi()
const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const emit = defineEmits(["closeDialog"])

const associationData = ref<WikidataAssociationCreation>({
  wikidataId: props.note.wikidataId!,
})
const conflictWikidataTitle = ref<string | undefined>(undefined)
const wikidataIdError = ref<string | undefined>(undefined)

const save = async () => {
  try {
    await props.storageAccessor
      .storedApi()
      .updateWikidataId(props.note.id, associationData.value)
    emit("closeDialog")
  } catch (e: unknown) {
    if (typeof e === "object" && e !== null && "wikidataId" in e) {
      wikidataIdError.value = (e as WikidataIdError).wikidataId
    } else {
      wikidataIdError.value = "An unknown error occurred"
    }
  }
}

const validateAndSave = async () => {
  try {
    const res =
      await managedApi.restWikidataController.fetchWikidataEntityDataById(
        associationData.value.wikidataId
      )
    if (
      res.WikidataTitleInEnglish !== "" &&
      res.WikidataTitleInEnglish.toUpperCase() !==
        props.note.noteTopology.titleOrPredicate.toUpperCase()
    ) {
      conflictWikidataTitle.value = res.WikidataTitleInEnglish
      return
    }
  } catch (e: unknown) {
    if (
      e instanceof Error &&
      "body" in e &&
      typeof e.body === "object" &&
      e.body &&
      "message" in e.body
    ) {
      wikidataIdError.value = (e as WikidataError).body.message
    } else {
      wikidataIdError.value = "An unknown error occurred"
    }
  }
  await save()
}
</script>
