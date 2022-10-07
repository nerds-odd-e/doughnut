<template>
  <h5 v-if="titleAsLink" class="header note-card-title">
    <NoteTitleWithLink :note="note" class="card-title" />
  </h5>
  <div v-else style="display: flex">
    <EditableText
      role="title"
      class="note-title"
      :multiple-line="false"
      scope-name="note"
      v-model="textContent.title"
      @blur="onBlurTextField"
    />
    <NoteWikidataAssociation
      v-if="note.wikidataId"
      :wikidata-id="note.wikidataId"
    />
  </div>
  <div class="note-content">
    <div
      v-if="!!note.location"
      class="map-applet"
      :data-lat="note.location.latitude"
      :data-lon="note.location.longitude"
    >
      <p>
        Insert Map here ¯|_(ツ)_ /¯
        Longitude: {{note.location.longitude}}
        Latitude: {{note.location.latitude}}
      </p>
    </div>
    <EditableText
      :multiple-line="true"
      role="description"
      v-if="size === 'large'"
      class="col note-description"
      scope-name="note"
      v-model="textContent.description"
      @blur="onBlurTextField"
    />
    <NoteShortDescription
      class="col"
      v-if="size === 'medium'"
      :short-description="note.shortDescription"
    />
    <SvgDescriptionIndicator
      v-if="size === 'small' && !!textContent.description"
      class="description-indicator"
    />
    <template v-if="note.pictureWithMask">
      <ShowPicture
        v-if="size !== 'small'"
        class="col text-center"
        v-bind="note.pictureWithMask"
        :opacity="0.2"
      />
      <SvgPictureIndicator v-else class="picture-indicator" />
    </template>
    <template v-if="!!note.noteAccessories.url">
      <div v-if="size != 'small'">
        <label v-if="note.noteAccessories.urlIsVideo">Video Url:</label>
        <label v-else>Url:</label>
        <a :href="note.noteAccessories.url" target="_blank">{{
          note.noteAccessories.url
        }}</a>
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
import NoteWikidataAssociation from "./NoteWikidataAssociation.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
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
      this.storageAccessor
        .api()
        .updateTextContent(
          this.note.id,
          this.textContent,
          this.note.textContent
        );
    },
  },
});
</script>

<style lang="sass" scoped>
.note-content
  display: flex
  flex-direction: column
  flex-wrap: wrap
  .col
    flex: 1 1 auto
    width: 50%
</style>
