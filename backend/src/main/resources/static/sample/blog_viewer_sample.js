
var app = new Vue({
  el: '#app',
  data: {
    blogInfo: {},
    apiUrl: "../api/note/blog"
  },
  methods: {
    callApi: function () {
        axios
            .get(this.apiUrl)
                .then((res) => {
                    this.blogInfo = res.data;
                })
                .catch((res) => {
                    alert("Error");
                });
    }
  }
});