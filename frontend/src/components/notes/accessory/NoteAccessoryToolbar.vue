<template>
  <div class="btn-group btn-group-sm ms-auto">
    <PopButton title="edit note image">
      <template #button_face>
        <SvgImage />
      </template>
      <template #default="{ closer }">
        <NoteEditImageDialog
          v-bind="{ noteId }"
          @close-dialog="handleCloseDialog(closer, $event)"
        />
      </template>
    </PopButton>

    <PopButton title="edit note url">
      <template #button_face>
        <SvgUrlIndicator />
      </template>
      <template #default="{ closer }">
        <NoteEditUrlDialog
          v-bind="{ noteId }"
          @close-dialog="handleCloseDialog(closer, $event)"
        />
      </template>
    </PopButton>

    <PopButton title="Upload audio">
      <template #button_face>
        <SvgResume />
      </template>
      <template #default="{ closer }">
        <NoteEditUploadAudioDialog
          v-bind="{ noteId }"
          @close-dialog="handleCloseDialog(closer, $event)"
        />
      </template>
    </PopButton>
  </div>
</template>

<script lang="ts">
import { NoteAccessory } from "@/generated/backend"
import { defineComponent } from "vue"
import PopButton from "../../commons/Popups/PopButton.vue"
import SvgImage from "../../svgs/SvgImage.vue"
import SvgResume from "../../svgs/SvgResume.vue"
import SvgUrlIndicator from "../../svgs/SvgUrlIndicator.vue"
import NoteEditImageDialog from "./NoteEditImageDialog.vue"
import NoteEditUploadAudioDialog from "./NoteEditUploadAudioDialog.vue"
import NoteEditUrlDialog from "./NoteEditUrlDialog.vue"

export default defineComponent({
  props: {
    noteId: {
      type: Number,
      required: true,
    },
  },
  emits: ["note-accessory-updated"],
  components: {
    NoteEditUploadAudioDialog,
    NoteEditImageDialog,
    NoteEditUrlDialog,
    SvgResume,
    SvgImage,
    SvgUrlIndicator,
    PopButton,
  },
  methods: {
    handleCloseDialog(closer, na: NoteAccessory) {
      if (na) {
        this.$emit("note-accessory-updated", na)
      }
      closer()
    },
  },
})
</script>
