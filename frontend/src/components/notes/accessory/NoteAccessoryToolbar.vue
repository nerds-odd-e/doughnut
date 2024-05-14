<template>
  <ToolbarFrame>
    <PopButton title="edit note image">
      <template #button_face>
        <SvgPictureIndicator />
      </template>
      <template #default="{ closer }">
        <NoteEditImageDialog
          v-bind="{ noteId }"
          @close-dialog="handleCloseDialog(closer)"
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
          @close-dialog="handleCloseDialog(closer)"
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
          @close-dialog="handleCloseDialog(closer)"
        />
      </template>
    </PopButton>
  </ToolbarFrame>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NoteEditUploadAudioDialog from "./NoteEditUploadAudioDialog.vue";
import PopButton from "../../commons/Popups/PopButton.vue";
import SvgResume from "../../svgs/SvgResume.vue";
import SvgPictureIndicator from "../../svgs/SvgPictureIndicator.vue";
import SvgUrlIndicator from "../../svgs/SvgUrlIndicator.vue";
import NoteEditImageDialog from "./NoteEditImageDialog.vue";
import NoteEditUrlDialog from "./NoteEditUrlDialog.vue";

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
    SvgPictureIndicator,
    SvgUrlIndicator,
    PopButton,
  },
  methods: {
    handleCloseDialog(closer) {
      this.$emit("note-accessory-updated");
      closer();
    },
  },
});
</script>
