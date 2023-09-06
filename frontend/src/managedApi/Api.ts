import RestfullFetch, { JsonData } from "./window/RestfulFetch";

class Api {
  fetch;

  constructor(baseUrl: string) {
    this.fetch = new RestfullFetch(baseUrl);
  }

  restGet(url: string) {
    return this.fetch.restRequest(url, {}, { method: "GET" });
  }

  restPost(url: string, data: JsonData) {
    return this.fetch.restRequest(url, data, { method: "POST" });
  }

  restPatch(url: string, data: JsonData) {
    return this.fetch.restRequest(url, data, { method: "PATCH" });
  }

  restDelete(url: string, data: JsonData) {
    return this.fetch.restRequest(url, data, { method: "DELETE" });
  }

  restPostMultiplePartForm(url: string, data: JsonData) {
    return this.fetch.restRequest(url, data, {
      method: "POST",
      contentType: "MultiplePartForm",
    });
  }

  restPatchMultiplePartForm(url: string, data: JsonData) {
    return this.fetch.restRequest(url, data, {
      method: "PATCH",
      contentType: "MultiplePartForm",
    });
  }

  restPostWithHtmlResponse(url: string, data: JsonData) {
    return this.fetch.restRequestWithHtmlResponse(url, data, {
      method: "POST",
    });
  }
}

export default Api;
