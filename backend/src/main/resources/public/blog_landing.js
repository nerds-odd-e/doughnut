
var app = new Vue({
  el: '#app',
  data: {
    blogInfo: [],
    apiUrl: "../api/note/blog"
  },
  created: function() {
    this.blogInfo.push(
        {
            "year": "2019",
            "link": "abc"
        }
    );
    /*axios
        .get(this.apiUrl)
            .then((res) => {
                this.blogInfo = res.data;
            })
            .catch((res) => {
                alert("Error");
            });*/
  }
});