import {
  restGet,
  restPost,
  restPatch,
  restPostMultiplePartForm,
  restPatchMultiplePartForm,
  restPostWithHtmlResponse,
} from '../restful/restful';

class ManagedApi {
  constructor(component, options={}) {
    this.component = component;
    this.skipLoading = options.skipLoading;
    this.base_url = '/api/'
  }

  around(promise) {
    const assignLoading = (value) => {
      if (this.skipLoading) return;
      if(this.component !== null && this.component !== undefined) {
        this.component.loading = value
      }
    }

    assignLoading(true);
    return new Promise((resolve, reject) => {
      promise.then(resolve).catch(reject).finally(()=>assignLoading(false))
    });
  }

  restGet(url) { return this.around(restGet(this.base_url + url)); }

  restPost(url, data) { return this.around(restPost(this.base_url + url, data));}

  restPatch(url, data) { return this.around(restPatch(this.base_url + url, data));}

  restPostMultiplePartForm(url, data) {return this.around(restPostMultiplePartForm(this.base_url + url, data));}

  restPatchMultiplePartForm(url, data) {return this.around(restPatchMultiplePartForm(this.base_url + url, data));}

  restPostWithHtmlResponse(url, data) {return this.around(restPostWithHtmlResponse(this.base_url + url, data));}
}

export default ManagedApi;