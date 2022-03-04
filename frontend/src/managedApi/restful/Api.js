import RestfullFetch from "./RestfulFetch";

class Api {
  constructor(base_url) {
    this.fetch = new RestfullFetch(base_url)
  }

  restGet(url) { return this.fetch.restRequest(url, {}, {}); }

  restPost(url, data) { return this.fetch.restRequest( url, data, { method: 'POST' }); }

  restPatch(url, data) { return this.fetch.restRequest( url, data, { method: 'PATCH' }); }

  restPostMultiplePartForm(url, data) {
     return this.fetch.restRequest( url, data, { method: 'POST', contentType: "MultiplePartForm" });
  }

  restPatchMultiplePartForm(url, data) {
    return this.fetch.restRequest( url, data, { method: 'PATCH', contentType: "MultiplePartForm" });
  }

  restPostWithHtmlResponse(url, data) {
    return this.fetch.restRequestWithHtmlResponse(url, data, { method: 'POST'});
  }
}

export default Api;
