import markdownizer from "@/components/form/markdownizer";

describe("Markdown and HTML Conversion Tests", () => {
  it("converts markdown to HTML correctly", () => {
    const markdown = "# Hello World\n\nThis is *markdown*.";
    const expectedHtml =
      "<h1>Hello World</h1><p>This is <em>markdown</em>.</p>";
    expect(markdownizer.markdownToHtml(markdown)).toBe(expectedHtml);
  });

  it("handles undefined markdown input", () => {
    expect(markdownizer.markdownToHtml(undefined)).toBe("");
  });

  describe("Html to markdown", () => {
    it("converts HTML to markdown correctly", () => {
      const html = "<h1>Hello World</h1><p>This is <em>markdown</em>.</p>";
      const expectedMarkdown =
        "Hello World\n===========\n\nThis is _markdown_.";
      expect(markdownizer.htmlToMarkdown(html)).toBe(expectedMarkdown);
    });
    it("converts empty lines with br correctly", () => {
      const html = "<p>a</p><p><br></p><p>b</p>";
      const expectedMarkdown = "a\n\n<p><br></p>\n\nb";
      expect(markdownizer.htmlToMarkdown(html)).toBe(expectedMarkdown);
    });
  });
});
