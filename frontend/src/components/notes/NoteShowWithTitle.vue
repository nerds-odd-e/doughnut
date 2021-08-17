<template>
    <LinkLists v-bind="{links, owns}">
      <div>
          <slot></slot>
          <div v-if="!!note.noteContent.url">
              <label v-if="note.noteContent.urlIsVideo">Video Url:</label>
              <label v-else>Url:</label>
              <a :href="note.noteContent.url">{{note.noteContent.url}}</a>
          </div>
      </div>
    <ShowPicture :note="note" :opacity="0.2"/>
    </LinkLists>
</template>

<script setup>
  import ShowPicture from "./ShowPicture.vue"
  import LinkLists from "../links/LinkLists.vue"
  const  propsx = defineProps({ note: Object, links: Object, level: Number, owns: { type: Boolean, required: true } })
</script>
