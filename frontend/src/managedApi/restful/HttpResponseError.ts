class HttpResponseError extends Error {
  constructor(status) {
    super(`got ${status}`);
    this.status = status;
  }
}

export default HttpResponseError;
