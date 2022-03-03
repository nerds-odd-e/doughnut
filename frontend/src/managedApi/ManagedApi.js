import {
  restGet,
  restPost,
  restPatch,
  restPostMultiplePartForm,
  restPatchMultiplePartForm,
  restPostWithHtmlResponse,
} from '../restful/restful';

class ManagedApi {
  constructor(component) {
    this.component = component;
  }

  around(promise) {
    if(this.component !== null && this.component !== undefined) {
      this.component.loading = true
    }
    return promise.finally(()=>{
      if(this.component !== null && this.component !== undefined) {
        this.component.loading = false
      }
    })

  }

  restGet(url) { return this.around(restGet(url)); }

  restPost(url, data) { return this.around(restPost(url, data));}

  restPatch(url, data) { return this.around(restPatch(url, data));}

  restPostMultiplePartForm(url, data) {return this.around(restPostMultiplePartForm(url, data));}

  restPatchMultiplePartForm(url, data) {return this.around(restPatchMultiplePartForm(url, data));}

  restPostWithHtmlResponse(url, data) {return this.around(restPostWithHtmlResponse(url, data));}
}

export default ManagedApi;