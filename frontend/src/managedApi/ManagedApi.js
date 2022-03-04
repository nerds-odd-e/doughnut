import {
  Api,
} from './restful/restful';

class ManagedApi {
  constructor(component, options={}) {
    this.api = new Api('/api/')
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

  restGet(url) { return this.around(this.api.restGet(url)); }

  restPost(url, data) { return this.around(this.api.restPost(url, data));}

  restPatch(url, data) { return this.around(this.api.restPatch(url, data));}

  restPostMultiplePartForm(url, data) {return this.around(this.api.restPostMultiplePartForm(url, data));}

  restPatchMultiplePartForm(url, data) {return this.around(this.api.restPatchMultiplePartForm(url, data));}

  restPostWithHtmlResponse(url, data) {return this.around(this.api.restPostWithHtmlResponse(url, data));}
}

export default ManagedApi;