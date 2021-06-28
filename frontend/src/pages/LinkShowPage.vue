<template>
  <LoadingPage v-bind="{loading, contentExists: !!linkViewedByUser}">
    <div v-if="linkViewedByUser">
        <LinkShow v-bind="linkViewedByUser">
            <div class="link-content">
              <div>
                <Select v-if="!!staticInfo" scopeName='link' field='linkType' v-model="formData.typeId" :errors="formErrors.typeId" :options="staticInfo.linkTypeOptions"/>
                <button class="btn btn-primary" v-on:click="updateLink()">Update</button>
                <button class="btn btn-danger" v-on:click="deleteLink()">Delete</button>
              </div>
              <nav class="nav d-flex flex-row-reverse p-0">
                <NoteStatisticsButton :linkid="linkViewedByUser.id"/>
              </nav>
            </div>
        </LinkShow>
    </div>
  </LoadingPage>
</template>

<script>
import LinkShow from "../components/links/LinkShow.vue"
import Select from "../components/form/Select.vue"
import NoteStatisticsButton from '../components/notes/NoteStatisticsButton.vue'
import LoadingPage from "./LoadingPage.vue"
import {restGet, restPost } from "../restful/restful"
import { relativeRoutePush } from "../routes/relative_routes"

export default {
  name: "LinkShowPage",
  components: {LinkShow, Select, NoteStatisticsButton, LoadingPage},
  props: {linkid: Number, staticInfo: Array},
  data() {
    return {
      linkViewedByUser: null,
      loading: null,
      formErrors: null
    }
  },
  computed: { formData(){ return !this.linkViewedByUser ? null : {typeId: this.linkViewedByUser.linkTypeId} }},
  methods: {
    fetchData() {
      restGet(`/api/links/${this.linkid}`, r=>this.loading=r, (res) => this.linkViewedByUser = res)
    },

    updateLink() {
      restPost(
        `/api/links/${this.linkid}`,
        this.formData,
        r=>this.loading=r,
        (res) => relativeRoutePush(this.$router, {name: "noteShow", params: { noteid: res.noteId}}),
        (res) => this.formErrors = res,
      )
    },

    deleteLink() {
      if(!confirm('Are you sure to delete this link?')) return;
      restPost(
        `/api/links/${this.linkid}/delete`, null, r=>this.loading=r,
        (res) => relativeRoutePush(this.$router, {name: "noteShow", replace: true, params: { noteid: res.noteId}}))
    }
  },
  watch: {
    linkid() {
      this.fetchData()
    }
  },
  mounted() {
    this.fetchData()
  }
}
</script>
