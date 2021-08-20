<template>
  <LoadingPage v-bind="{loading, contentExists: !!circle}">
    <div v-if="circle">
        <h1 v-text="circle.name"/>
        <p> <a class="nav-link" :href="`/circles/${circle.id}/notebooks/new`">Add New Notebook In This Circle</a> </p>


        <NotebookCardsWithButtons :notebooks="circle.notebooks">
          <template #default="{notebook}">
            <NotebookButtons v-bind="{notebook}" class="card-header-btn">
              <template #additional-buttons>
                <BazaarNotebookButtons :notebook="notebook" />
              </template>
            </NotebookButtons>
          </template>
        </NotebookCardsWithButtons>

        <nav class="nav justify-content-end">
            <div class="nav-item circle-member" v-for="member in circle.members" :key="member.id">
               <span :title="member.name"> <SvgMissingAvatar/> </span>
            </div>
        </nav>

        <h2>Invite People To Your Circle</h2>
        Please share this invitation code so that they can join your circle:

        <div class="jumbotron">
            <input id="invitation-code" :value="invitationUrl" readonly/>
        </div>
    </div>
  </LoadingPage>
</template>

<script>
import SvgMissingAvatar from "../components/svgs/SvgMissingAvatar.vue"
import LoadingPage from "./commons/LoadingPage.vue"
import NotebookCardsWithButtons from "../components/notebook/NotebookCardsWithButtons.vue"
import NotebookButtons from "../components/notebook/NotebookButtons.vue"
import BazaarNotebookButtons from "../components/bazaar/BazaarNotebookButtons.vue"
import {restGet, restPost } from "../restful/restful"

export default {
  components: {SvgMissingAvatar, NotebookCardsWithButtons, NotebookButtons, BazaarNotebookButtons, LoadingPage},
  props: {circleId: Number},

  data() {
    return {
      circle: null,
      loading: null,
      formErrors: {}
    }
  },

  methods: {
    fetchData() {
      restGet(`/api/circles/${this.circleId}`, r=>this.loading=r).then(res => this.circle = res)
    },
  },

  computed: {
    invitationUrl() {
      return `${window.location.origin}/circles/join/${this.circle.invitationCode}`
    }

  },

  watch: {
    circleId() {
      this.fetchData()
    }
  },

  mounted() {
    this.fetchData()
  },
}
</script>

<style lang="sass" scoped>
#invitation-code
  width: 100%
</style>