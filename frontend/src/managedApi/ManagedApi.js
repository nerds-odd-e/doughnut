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

  restGet(url) { return this.around(restGet(url)); }

  restPost(url, data) { return this.around(restPost(url, data));}

  restPatch(url, data) { return this.around(restPatch(url, data));}

  restPostMultiplePartForm(url, data) {return this.around(restPostMultiplePartForm(url, data));}

  restPatchMultiplePartForm(url, data) {return this.around(restPatchMultiplePartForm(url, data));}

  restPostWithHtmlResponse(url, data) {return this.around(restPostWithHtmlResponse(url, data));}
}

export default ManagedApi;