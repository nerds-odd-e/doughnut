<template>
  <div class="more-options-container daisy-bg-base-200 animate-dropdown">
    <div class="header-container">
      <h3 class="dialog-title">More Options</h3>
      <button class="close-btn" @click="closeDialog" title="Close">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
          <path d="M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z"/>
        </svg>
      </button>
    </div>
    <div class="options-list">
      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-w-full daisy-justify-start"
        title="Note Recall Settings"
      >
        <template #button_face>
          <span>Note Recall Settings</span>
        </template>
        <template #default>
          <NoteInfoBar v-bind="{ noteId: note.id }" />
        </template>
      </PopButton>

      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-w-full daisy-justify-start"
        title="Generate Image with DALL-E"
      >
        <template #button_face>
          <span>Generate Image with DALL-E</span>
        </template>
        <template #default>
          <AIGenerateImageDialog v-bind="{ note }" />
        </template>
      </PopButton>

      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-w-full daisy-justify-start"
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

      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-w-full daisy-justify-start"
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

      <PopButton btn-class="daisy-btn daisy-btn-ghost daisy-w-full daisy-justify-start" title="Export...">
        <template #button_face>
          <SvgExport />
          <span class="ms-2">Export...</span>
        </template>
        <template #default="{ closer }">
          <NoteExportDialog :note="note" @close-dialog="closer" />
        </template>
      </PopButton>

      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-w-full daisy-justify-start"
        title="Questions for the note"
      >
        <template #button_face>
          <span>Questions for the note</span>
        </template>
        <template #default>
          <Questions v-bind="{ note }" />
        </template>
      </PopButton>

      <NoteDeleteButton
        class="daisy-btn daisy-btn-ghost daisy-w-full daisy-justify-start"
        v-bind="{ noteId: note.id }"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Note } from "@generated/backend"
import type { NoteAccessory } from "@generated/backend"
import PopButton from "../../commons/Popups/PopButton.vue"
import AIGenerateImageDialog from "../AIGenerateImageDialog.vue"
import Questions from "../Questions.vue"
import NoteInfoBar from "../NoteInfoBar.vue"
import SvgImage from "../../svgs/SvgImage.vue"
import SvgUrlIndicator from "../../svgs/SvgUrlIndicator.vue"
import NoteEditImageDialog from "./NoteEditImageDialog.vue"
import NoteEditUrlDialog from "./NoteEditUrlDialog.vue"
import NoteDeleteButton from "../core/NoteDeleteButton.vue"
import SvgExport from "../../svgs/SvgExport.vue"
import NoteExportDialog from "../core/NoteExportDialog.vue"

const { note } = defineProps<{
  note: Note
}>()

const emit = defineEmits<{
  (e: "close-dialog"): void
  (e: "note-accessory-updated", na: NoteAccessory): void
}>()

const closeDialog = () => {
  emit("close-dialog")
}

const noteAccessoriesUpdated = (closer: () => void, na: NoteAccessory) => {
  if (na) {
    emit("note-accessory-updated", na)
  }
  closer()
}
</script>

<style scoped>
.more-options-container {
  position: relative;
  border-radius: 0 0 12px 12px;
  padding: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  animation: dropDown 0.3s ease-out;
  transform-origin: top;
}

@keyframes dropDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.header-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid hsl(var(--bc) / 0.1);
}

.dialog-title {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0;
  color: hsl(var(--bc));
}

.close-btn {
  flex-shrink: 0;
  background: none;
  border: none;
  cursor: pointer;
  color: hsl(var(--bc) / 0.7);
  transition: color 0.3s ease, background-color 0.3s ease;
  padding: 8px;
  border-radius: 50%;
  background-color: hsl(var(--bc) / 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: hsl(var(--bc));
  background-color: hsl(var(--bc) / 0.1);
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.options-list :deep(.daisy-btn) {
  text-align: left;
  padding: 12px 16px;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.options-list :deep(.daisy-btn:hover) {
  background-color: hsl(var(--bc) / 0.1);
}
</style>

