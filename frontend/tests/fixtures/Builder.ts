abstract class Builder<T=any, PB extends Builder | undefined=any> {
  protected parentBuilder: PB | undefined;

  protected childrenBuilders: Builder[];

  constructor(parentBuilder?: PB) {
    this.parentBuilder = parentBuilder;
    this.childrenBuilders = [];
  }

  get and(): PB {
    if (!this.parentBuilder) {
      throw new Error("There is no parent builder");
    }
    return this.parentBuilder;
  }

  please(): T {
    if (this.parentBuilder !== undefined) {
      return this.parentBuilder.please();
    }
    return this.do();
  }

  abstract do(): T;
}

export default Builder;
