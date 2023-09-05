<template>
  <div>
    <h1>JSONL Table Viewer</h1>
    <table>
      <thead>
        <tr>
          <th>Name</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td>{{ item.name }}</td>
        </tr>
      </tbody>
    </table>
    <button @click="downloadJsonl">Download JSONL</button>
  </div>
</template>

<script>
export default {
  data() {
    return {
      items: [], // Store fetched data here
    };
  },
  methods: {
    async fetchJsonlData() {
      try {
        const response = await fetch("/api/gettrainingdata/goodtrainingdata"); // Replace with your API endpoint
        // "https://jsonplaceholder.typicode.com/posts"
        if (!response.ok) {
          throw new Error("Network response was not ok");
        }
        const jsonlData = await response.text();
        // Parse the JSONL data into an array of objects
        const lines = jsonlData.split("\n");
        this.items = lines.map((line) => JSON.parse(line.trim()));
      } catch (error) {
        // console.error("Error fetching JSONL data:", error);
      }
    },
    downloadJsonl() {
      // Create a Blob with the JSONL data
      const jsonlData = this.items
        .map((item) => JSON.stringify(item))
        .join("\n");
      const blob = new Blob([jsonlData], { type: "text/plain" });

      // Create a link element and trigger a download
      const a = document.createElement("a");
      a.href = URL.createObjectURL(blob);
      a.download = "data.jsonl";
      a.style.display = "none";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    },
  },
  mounted() {
    this.fetchJsonlData();
  },
};
</script>

<style lang="scss" scoped>
.is-correct {
  font-weight: bold;
  background-color: #00ff00;
}

.is-incorrect {
  font-weight: bold;
  background-color: #ff0000;
}

span {
  display: block;
  overflow: hidden;
  padding-right: 5px;
}

.chat-input-container {
  width: 100%;
  display: flex;
  flex-direction: row;
  padding-top: 5px;
  padding-bottom: 5px;
}

.chat-input-text {
  width: 100%;
  margin-right: 5px;
  flex-grow: 1;
}
input.auto-extendable-input {
  width: 100%;
}

.float-btn {
  float: right;
}

.chat-answer-container {
  display: flex;
  margin: 2% 0;
}

.chat-answer-icon {
  width: 6%;
  height: 6%;
}

.chat-answer-text {
  position: relative;
  display: inline-block;
  margin-left: 15px;
  padding: 7px 10px;
  width: 100%;
  border: solid 3px #555;
  box-sizing: border-box;
}

.chat-answer-text:before {
  content: "";
  position: absolute;
  top: 50%;
  left: -24px;
  margin-top: -12px;
  border: 12px solid transparent;
  border-right: 12px solid #fff;
  z-index: 2;
}

.chat-answer-text:after {
  content: "";
  position: absolute;
  top: 50%;
  left: -30px;
  margin-top: -14px;
  border: 14px solid transparent;
  border-right: 14px solid #555;
}

.chat-answer-text p {
  margin: 0;
  word-break: break-word;
}

.chat-control {
  width: calc(100% - 140px);
  margin-left: auto;
  margin-right: 40px;
}
</style>
