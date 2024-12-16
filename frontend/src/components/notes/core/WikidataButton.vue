<template>
  <div v-if="note.wikidataId">
    <div class="daisy-dropdown">
      <button
        aria-expanded="false"
        aria-haspopup="true"
        class="daisy-btn daisy-dropdown-toggle"
        tabindex="0"
        role="button"
        title="wikidata options"
      >
        <SvgWikidata />
      </button>

      <ul class="daisy-dropdown-content daisy-menu daisy-p-2 daisy-bg-base-300 daisy-rounded-box daisy-w-52 daisy-shadow daisy-z-50">
        <li>
          <NoteWikidataAssociation :wikidata-id="note.wikidataId" />
        </li>
        <li>
          <PopButton title="associate wikidata">
            <template #button_face>
              <SvgWikidata />
              Edit Wikidata ID
            </template>
            <template #default="{ closer }">
              <WikidataAssociationDialog
                v-bind="{ note, storageAccessor }"
                @close-dialog="closer"
              />
            </template>
          </PopButton>
        </li>
      </ul>
    </div>
  </div>
  <PopButton v-else title="associate wikidata">
    <template #button_face>
      <SvgWikidata />
      {{ title || "" }}
    </template>
    <template #default="{ closer }">
      <WikidataAssociationDialog
        v-bind="{ note, storageAccessor }"
        @close-dialog="closer"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import type { Note } from "@/generated/backend"
import type { StorageAccessor } from "@/store/createNoteStorage"
import PopButton from "../../commons/Popups/PopButton.vue"
import SvgWikidata from "../../svgs/SvgWikidata.vue"
import WikidataAssociationDialog from "../WikidataAssociationDialog.vue"
import NoteWikidataAssociation from "../NoteWikidataAssociation.vue"

defineProps<{
  storageAccessor: StorageAccessor
  note: Note
  title?: string
}>()
</script>
