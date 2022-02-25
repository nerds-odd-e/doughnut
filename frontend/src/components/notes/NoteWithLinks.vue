<template>
  <NoteShell
    class="note-body"
    v-bind="{ id: note.id, updatedAt: note.textContent?.updatedAt }"
  >
    <NoteFrameOfLinks v-bind="{ links: note.links }">
      <NoteContent v-bind="{ note }" />
    </NoteFrameOfLinks>
  </NoteShell>
  <NoteShowCommentButton v-if="featureToggle" :comments='comments'/>
</template>

<script>
import EditableText from "../form/EditableText.vue";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteShell from "./NoteShell.vue";
import NoteContent from "./NoteContent.vue";
import NoteShowCommentButton from "./NoteShowCommentButton.vue";
import { restGet } from "../../restful/restful";
import Stomp from "webstomp-client";
import SockJS from "sockjs-client/dist/sockjs.min.js";

export default {
  name: "NoteWithLinks",
  data() {
    return { comments: [] }
  },
  props: {
    note: Object,
  },
  mounted() {
    this.fetchData();
    this.connect();
  },
  components: {
    NoteFrameOfLinks,
    NoteShell,
    NoteContent,
    EditableText,
    NoteShowCommentButton,
  },
  methods: {
    connect() {
      this.socket = new SockJS("http://localhost:3000/websocket");
      this.stompClient = Stomp.over(this.socket);
      this.stompClient.connect(
        {},
        frame => {
          this.connected = true;
          console.log(frame);
          this.stompClient.subscribe("/topic/greetings", tick => {
            console.log(tick);
            this.received_messages.push(JSON.parse(tick.body).content);
          });
        },
        error => {
          console.log(error);
          this.connected = false;
        }
      );
    },
    disconnect() {
      if (this.stompClient) {
        this.stompClient.disconnect();
      }
      this.connected = false;
    },
    fetchData() {
      restGet(`/api/comments/${this.note.id}`)
        .then((res) => {
          this.comments = res
        })
        .catch(() => {});
    }
  },
  computed: {
    featureToggle() {
      return this.$store.getters.getFeatureToggle();
    }
  }
};
</script>

<style scoped>
.note-body {
  padding-left: 10px;
  padding-right: 10px;
  border-radius: 10px;
  border-style: solid;
  border-top-width: 3px;
  border-bottom-width: 1px;
  border-right-width: 3px;
  border-left-width: 1px;
}

.note-title {
  margin-top: 0px;
  padding-top: 10px;
  color: black;
}

.outdated-label {
  display: inline-block;
  height: 100%;
  vertical-align: middle;
  margin-left: 20px;
  padding-bottom: 10px;
  color: red;
}
</style>
