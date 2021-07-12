<template>
  <LoadingPage v-bind="{loading, contentExists: !!linkViewedByUser}">
    <div v-if="linkViewedByUser">
        <LinkShow v-bind="linkViewedByUser">

            <div class="link-content">
              <div>
                <LinkTypeSelect scopeName='link' v-model="formData.typeId" :errors="formErrors.typeId"/>
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
import LinkTypeSelect from "../components/links/LinkTypeSelect.vue"
import NoteStatisticsButton from '../components/notes/NoteStatisticsButton.vue'
import LoadingPage from "./commons/LoadingPage.vue"
import {restGet, restPost } from "../restful/restful"

export default {
  name: "LinkShowPage",
  components: {LinkShow, LinkTypeSelect, NoteStatisticsButton, LoadingPage},
  props: {linkid: Number},
  data() {
    return {
      linkViewedByUser: null,
      loading: null,
      formErrors: {}
    }
  },
  computed: { formData(){ return !this.linkViewedByUser ? null : {typeId: this.linkViewedByUser.linkTypeId} }},
  methods: {
    fetchData() {
      restGet(`/api/links/${this.linkid}`, r=>this.loading=r).then(res => this.linkViewedByUser = res)
    },

    updateLink() {
      restPost(
        `/api/links/${this.linkid}`,
        this.formData,
        r=>this.loading=r)
        .then(res => this.$router.push({name: "noteShow", params: { noteId: res.noteId}}))
        .catch(res => this.formErrors = res)
    },

    async deleteLink() {
      if(!await this.$popups.confirm('Are you sure to delete this link?')) return;
      restPost(`/api/links/${this.linkid}/delete`, null, r=>this.loading=r)
        .then(res => this.$router.push({name: "noteShow", replace: true, params: { noteId: res.noteId}}))
    }
  },
  watch: {
    linkid() {
      this.fetchData()
    }
  },
  mounted() {
    this.fetchData()
  },
}
</script>
