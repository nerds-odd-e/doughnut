<template>
  <div class="alert alert-warning" v-if="note.deletedAt">
    This note has been deleted
  </div>
  <h5 v-if="titleAsLink" class="header note-card-title">
    <NoteTitleWithLink :note="note" class="card-title" />
  </h5>
  <div v-else style="display: flex">
    <EditableText
      role="title"
      class="note-title"
      scope-name="note"
      v-model="textContent.title"
      @blur="onBlurTextField"
    />
    <div class="header-options">
      <NoteWikidataAssociation
        v-if="note.wikidataId"
        :wikidata-id="note.wikidataId"
      />
      <span> </span>
    </div>
  </div>
  <div class="note-content">
    <DescriptionEditor
      :multiple-line="true"
      role="description"
      v-if="size === 'large'"
      class="note-description"
      scope-name="note"
      v-model="textContent.description"
      @blur="onBlurTextField"
    />
    <NoteShortDescription
      v-if="size === 'medium'"
      :description="note.textContent.description"
    />
    <SvgDescriptionIndicator
      v-if="size === 'small' && !!textContent.description"
      class="description-indicator"
    />
    <template v-if="note.pictureWithMask">
      <ShowPicture
        v-if="size !== 'small'"
        class="text-center"
        v-bind="note.pictureWithMask"
        :opacity="0.2"
      />
      <SvgPictureIndicator v-else class="picture-indicator" />
    </template>
    <template v-if="!!note.noteAccessories.url">
      <div v-if="size != 'small'">
        <label
          id="note-url"
          v-text="note.noteAccessories.urlIsVideo ? 'Video Url:' : 'Url:'"
        />
        <a
          aria-labelledby="note-url"
          :href="note.noteAccessories.url"
          target="_blank"
          >{{ note.noteAccessories.url }}</a
        >
      </div>
      <a v-else :href="note.noteAccessories.url" target="_blank">
        <SvgUrlIndicator />
      </a>
    </template>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteTitleWithLink from "./NoteTitleWithLink.vue";
import NoteShortDescription from "./NoteShortDescription.vue";
import ShowPicture from "./ShowPicture.vue";
import SvgDescriptionIndicator from "../svgs/SvgDescriptionIndicator.vue";
import SvgPictureIndicator from "../svgs/SvgPictureIndicator.vue";
import SvgUrlIndicator from "../svgs/SvgUrlIndicator.vue";
import EditableText from "../form/EditableText.vue";
import DescriptionEditor from "../form/DescriptionEditor.vue";
import NoteWikidataAssociation from "./NoteWikidataAssociation.vue";
import type { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup() {
    return {
      submitChange: (() => {}) as () => void,
    };
  },
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    size: { type: String, default: "large" },
    titleAsLink: Boolean,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    NoteShortDescription,
    ShowPicture,
    SvgDescriptionIndicator,
    SvgPictureIndicator,
    SvgUrlIndicator,
    EditableText,
    DescriptionEditor,
    NoteTitleWithLink,
    NoteWikidataAssociation,
  },
  computed: {
    textContent() {
      return { ...this.note.textContent };
    },
  },
  methods: {
    onBlurTextField() {
      this.submitChange();
    },
  },
  mounted() {
    this.submitChange = () => {
      this.storageAccessor
        .api(this.$router)
        .updateTextContent(
          this.note.id,
          this.textContent,
          this.note.textContent
        );
    };
  },
});
</script>

<style lang="sass" scoped>
.header-options
  display: flex
  align-items: center
  margin: 0 10px
</style>
