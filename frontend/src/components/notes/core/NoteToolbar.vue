<template>
  <nav class="daisy-navbar daisy-bg-base-200">
    <div class="daisy-btn-group daisy-btn-group-sm">
      <NoteNewButton
        button-title="Add Child Note"
        v-bind="{ referenceNote: note, insertMode: 'as-child', storageAccessor }"
      >
        <SvgAddChild />
      </NoteNewButton>

      <NoteNewButton
        v-if="note.parentId"
        button-title="Add Next Sibling Note"
        v-bind="{ referenceNote: note, insertMode: 'after', storageAccessor }"
      >
        <SvgAddSibling />
      </NoteNewButton>

      <PopButton title="search and link note">
        <template #button_face>
          <SvgSearchForLink />
        </template>
        <template #default="{ closer }">
          <LinkNoteDialog
            v-bind="{ note, storageAccessor }"
            @close-dialog="closer"
          />
        </template>
      </PopButton>

      <a
        v-if="!conversationButton"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        role="button"
        @click="() => router.push({
          name: 'noteShow',
          params: { noteId: note.id },
          query: { conversation: 'true' }
        })"
        title="Star a conversation about this note"
      >
        <SvgChat />
      </a>

      <div v-if="note.wikidataId">
        <div class="daisy-dropdown">
          <label
            role="button"
            tabindex="0"
            class="daisy-btn daisy-btn-ghost daisy-btn-sm"
            title="wikidata options"
          >
            <SvgWikidata />
          </label>

          <ul tabindex="0" class="daisy-dropdown-content daisy-menu daisy-p-2 daisy-bg-base-300 daisy-rounded-box daisy-w-52 daisy-shadow daisy-z-50">
            <li class="daisy-menu-item">
              <NoteWikidataAssociation :wikidata-id="note.wikidataId" />
            </li>
            <li class="daisy-menu-item">
              <PopButton title="associate wikidata" class="w-full">
                <template #button_face>
                  <SvgWikidata />
                  <span class="ms-2">Edit Wikidata ID</span>
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
        </template>
        <template #default="{ closer }">
          <WikidataAssociationDialog
            v-bind="{ note, storageAccessor }"
            @close-dialog="closer"
          />
        </template>
      </PopButton>

      <button v-if="!asMarkdown" class="daisy-btn daisy-btn-ghost daisy-btn-sm" title="Edit as markdown" @click="$emit('edit-as-markdown', true)">
        <SvgMarkdown />
      </button>
      <button v-else class="daisy-btn daisy-btn-ghost daisy-btn-sm" title="Edit as rich content" @click="$emit('edit-as-markdown', false)">
        <SvgRichContent />
      </button>

      <button class="daisy-btn daisy-btn-ghost daisy-btn-sm" title="Audio tools" v-if="!audioTools" @click="audioTools = true">
        <SvgAudioInput />
      </button>

      <div class="daisy-dropdown daisy-dropdown-end">
        <button
          tabindex="0"
          class="daisy-btn daisy-btn-ghost daisy-btn-sm"
          title="more options"
        >
          <SvgCog />
        </button>
        <ul tabindex="0" class="daisy-dropdown-content daisy-menu daisy-p-2 daisy-bg-base-300 daisy-rounded-box daisy-w-52 daisy-shadow daisy-z-50">
          <li>
            <PopButton
              btn-class="daisy-w-full"
              title="Note Recall Settings"
            >
              <NoteInfoBar v-bind="{ noteId: note.id }" />
            </PopButton>
          </li>

          <li>
            <PopButton
              btn-class="daisy-w-full"
              title="Generate Image with DALL-E"
            >
              <AIGenerateImageDialog v-bind="{ note, storageAccessor }" />
            </PopButton>
          </li>

          <li>
            <PopButton
              btn-class="daisy-w-full"
              title="Edit Note Image"
            >
              <template #button_face>
                <SvgImage />
                <span class="ms-2">Edit Note Image</span>
              </template>
              <template #default="{ closer }">
                <NoteEditImageDialog
                  v-bind="{ noteId: note.id }"
                  @close-dialog="noteAccessoriesUpdated(closer, $event)"
                />
              </template>
            </PopButton>
          </li>

          <li>
            <PopButton
              btn-class="daisy-w-full"
              title="Edit Note URL"
            >
              <template #button_face>
                <SvgUrlIndicator />
                <span class="ms-2">Edit Note URL</span>
              </template>
              <template #default="{ closer }">
                <NoteEditUrlDialog
                  v-bind="{ noteId: note.id }"
                  @close-dialog="noteAccessoriesUpdated(closer, $event)"
                />
              </template>
            </PopButton>
          </li>

          <li>
            <PopButton
              btn-class="daisy-w-full"
              title="Questions for the note"
            >
              <Questions v-bind="{ note }" />
            </PopButton>
          </li>
          <li>
            <NoteDeleteButton
              class="daisy-w-full"
              v-bind="{ noteId: note.id, storageAccessor }"
            />
          </li>
        </ul>
      </div>
    </div>
  </nav>
  <NoteAudioTools
    v-if="audioTools"
    v-bind="{ note, storageAccessor }"
    @close-dialog="audioTools = false"
  />

</template>

<script setup lang="ts">
import { ref } from "vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { Note } from "@/generated/backend"
import type { NoteAccessory } from "@/generated/backend"
import NoteNewButton from "./NoteNewButton.vue"
import SvgAddChild from "../../svgs/SvgAddChild.vue"
import SvgAddSibling from "../../svgs/SvgAddSibling.vue"
import SvgSearchForLink from "../../svgs/SvgSearchForLink.vue"
import LinkNoteDialog from "../../links/LinkNoteDialog.vue"
import SvgCog from "../../svgs/SvgCog.vue"
import NoteDeleteButton from "./NoteDeleteButton.vue"
import PopButton from "../../commons/Popups/PopButton.vue"
import AIGenerateImageDialog from "../AIGenerateImageDialog.vue"
import Questions from "../Questions.vue"
import NoteInfoBar from "../NoteInfoBar.vue"
import SvgMarkdown from "@/components/svgs/SvgMarkdown.vue"
import SvgRichContent from "@/components/svgs/SvgRichContent.vue"
import SvgImage from "../../svgs/SvgImage.vue"
import SvgAudioInput from "../../svgs/SvgAudioInput.vue"
import SvgUrlIndicator from "../../svgs/SvgUrlIndicator.vue"
import NoteEditImageDialog from "../accessory/NoteEditImageDialog.vue"
import NoteEditUrlDialog from "../accessory/NoteEditUrlDialog.vue"
import NoteAudioTools from "../accessory/NoteAudioTools.vue"
import SvgRobot from "@/components/svgs/SvgRobot.vue"
import { useRouter } from "vue-router"
import SvgChat from "@/components/svgs/SvgChat.vue"
import SvgWikidata from "../../svgs/SvgWikidata.vue"
import WikidataAssociationDialog from "../WikidataAssociationDialog.vue"
import NoteWikidataAssociation from "../NoteWikidataAssociation.vue"

const { storageAccessor, note } = defineProps<{
  storageAccessor: StorageAccessor
  note: Note
  asMarkdown?: boolean
  conversationButton?: boolean
}>()

const audioTools = ref(false)

const router = useRouter()

const emit = defineEmits(["note-accessory-updated", "edit-as-markdown"])

const noteAccessoriesUpdated = (closer: () => void, na: NoteAccessory) => {
  if (na) {
    emit("note-accessory-updated", na)
  }
  closer()
}
</script>
