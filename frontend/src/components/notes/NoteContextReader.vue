<template>
  <NoteRealmLoader v-bind="{ noteId }">
    <template #default="{ noteRealm }">
      <ContentLoader v-if="!noteRealm" />
      <article v-else aria-label="Note context" class="flex flex-col gap-2">
        <BreadcrumbWithCircle
          :notebook-realm="noteRealm.notebookRealm"
          :ancestor-folders="noteRealm.ancestorFolders ?? []"
        />
        <NoteTextContent
          :note="noteRealm.note"
          :wiki-links="noteRealm.wikiLinks ?? []"
        />
        <router-link :to="noteShowLocation(noteId)">Open full note</router-link>
      </article>
    </template>
  </NoteRealmLoader>
</template>

<script setup lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"
import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"
import { noteShowLocation } from "@/routes/noteShowLocation"
import NoteRealmLoader from "./NoteRealmLoader.vue"
import NoteTextContent from "./core/NoteTextContent.vue"

defineProps({
  noteId: { type: Number, required: true },
})
</script>
