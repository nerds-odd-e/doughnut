import Builder from "./Builder";


class NotePositionBuilder extends Builder {
  data: any;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.data = {
      owns: true,
      ancestors: [],
      notebook: {
        ownership: {
          isFromCircle: false,
        }
      }
    }
  }

  inBazaar(): NotePositionBuilder {
    this.data.owns = false
    return this
  }

  inCircle(value: string): NotePositionBuilder {
    this.data.owns = true;
    this.data.notebook = {
      ownership: {
        isFromCircle: true,
        circle: {
          name: value,
        },
      },
    };
    return this;
  }


  do(): any {
    return this.data
  }
}

export default NotePositionBuilder;
