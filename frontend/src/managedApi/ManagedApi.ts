import Api from "./restful/Api";

interface ManaagedComponent {
  loading: boolean
}

class ManagedApi {
  api;

  component;

  skipLoading: boolean;

  constructor(component: ManaagedComponent, options: {skipLoading: boolean}={skipLoading: false}) {
    this.api = new Api('/api/')
    this.component = component;
    this.skipLoading = options.skipLoading;
  }

  around(promise: Promise<any>) {
    const assignLoading = (value: boolean) => {
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

  restGet(url: string) { return this.around(this.api.restGet(url)); }

  restPost(url: string, data: any) { return this.around(this.api.restPost(url, data));}

  restPatch(url: string, data: any) { return this.around(this.api.restPatch(url, data));}

  restPostMultiplePartForm(url: string, data: any) {return this.around(this.api.restPostMultiplePartForm(url, data));}

  restPatchMultiplePartForm(url: string, data: any) {return this.around(this.api.restPatchMultiplePartForm(url, data));}

  restPostWithHtmlResponse(url: string, data: any) {return this.around(this.api.restPostWithHtmlResponse(url, data));}
}

export default ManagedApi;