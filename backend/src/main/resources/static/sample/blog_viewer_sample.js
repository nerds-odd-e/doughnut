
var app = new Vue({
  el: '#app',
  data: {
    blogInfo: {}
  },
  methods: {
    callApi: function () {
        axios
            .get('../api/note/blog')
                .then((res) => {
                    this.blogInfo = res.data;
                });
    }
  }
});