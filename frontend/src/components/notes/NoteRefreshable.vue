<template>
  <NoteViewedByUserWithoutChildren v-bind="{note, links, ancestors, notebook, owns, ...this.refreshed }" @updated="refresh()"/>
</template>

<script>
import NoteViewedByUserWithoutChildren from "./NoteViewedByUserWithoutChildren.vue"
import {restGet} from "../../restful/restful"

export default {
  name: "NoteRefreshable",
  props: {
    note: Object,
    links: Object,
    ancestors: Array,
    notebook: Object,
    owns: Boolean
  },
  data() {
    return {
      refreshed: {}
    }
  },
  components: { NoteViewedByUserWithoutChildren },
  methods: { 
    refresh() {
      restGet(`/api/notes/${this.note.id}`, (r)=>{}, (res) => this.refreshed = res)
    }
  }
}

</script>
