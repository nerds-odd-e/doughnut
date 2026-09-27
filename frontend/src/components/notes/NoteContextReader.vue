<template>
  <NoteRealmLoader v-bind="{ noteId }">
    <template #default="{ noteRealm }">
      <ContentLoader v-if="!noteRealm" />
      <article
        v-else
        aria-label="Note context"
        class="flex min-w-0 flex-col gap-2"
      >
        <BreadcrumbWithCircle
          :notebook-realm="noteRealm.notebookRealm"
          :ancestor-folders="noteRealm.ancestorFolders ?? []"
        />
        <NoteTextContent
          :note="noteRealm.note"
          :wiki-links="noteRealm.wikiLinks ?? []"
        />
        <ShowImage :note="noteRealm.note" />
        <NoteReferences :note-topologies="noteRealm.references ?? []" />
        <router-link
          :to="
            focusedPropertyKey
              ? notePropertyLocation(noteId, focusedPropertyKey)
              : noteShowLocation(noteId)
          "
          >Open full note</router-link
        >
      </article>
    </template>
  </NoteRealmLoader>
</template>

<script setup lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"
import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"
import { provideFocusedPropertyKey } from "@/composables/useFocusedNoteProperty"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import NoteRealmLoader from "./NoteRealmLoader.vue"
import NoteTextContent from "./core/NoteTextContent.vue"
import NoteReferences from "./NoteReferences.vue"
import ShowImage from "./widgets/ShowImage.vue"

const props = defineProps({
  noteId: { type: Number, required: true },
  focusedPropertyKey: { type: String, default: undefined },
})

provideFocusedPropertyKey(() => props.focusedPropertyKey)
</script>
