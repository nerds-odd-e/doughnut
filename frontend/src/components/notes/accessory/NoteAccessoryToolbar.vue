<template>
  <ToolbarFrame>
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
import NoteEditUploadAudioDialog from "../NoteEditUploadAudioDialog.vue";
import PopButton from "../../commons/Popups/PopButton.vue";
import SvgResume from "../../svgs/SvgResume.vue";

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
    SvgResume,
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
