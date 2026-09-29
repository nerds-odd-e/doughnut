#!/usr/bin/env bash
set -euo pipefail

# Build epub_long_chapter_before_target.epub: a small EPUB whose chosen chapter follows a
# long chapter, shaped like Project Gutenberg books (each chapter body is wrapped in a
# <div class="chapter" id="..."> that the table of contents targets, and the contents page
# links to an <a id> inside the next chapter's heading).
#
# The long chapter before the chosen one makes landing sensitive to content epub.js
# inserts above the target after display, and the wrapper div makes the chapter's
# target span the whole chapter rather than its heading.
#
# A cover page comes first in the spine and no table-of-contents entry targets it (as in
# Project Gutenberg's wrap0000.xhtml), so the book's first block is the extractor's
# `*beginning*` block holding the cover.
#
# End matter after the chapters has table-of-contents entries without content of their
# own: a title and its detailed contents that share one start (as in Project Gutenberg's
# "ON THE ORIGIN OF SPECIES."), and a licence heading followed directly by its first
# section's target (as in Alice's "THE FULL PROJECT GUTENBERG LICENSE").
#
# When to re-run: after changing this script. The output is deterministic.
#
# From repo root:
#   ./e2e_test/fixtures/book_reading/regenerate_epub_long_chapter_before_target.sh

here=$(cd "$(dirname "$0")" && pwd)
out_epub=$here/epub_long_chapter_before_target.epub

python3 - "$out_epub" <<'PY'
import sys
import zipfile

out = sys.argv[1]
STAMP = (2026, 1, 1, 0, 0, 0)


def paragraphs(label, count):
    return "\n".join(
        f"  <p>{label} paragraph {n}. The quick brown fox jumps over the lazy dog, "
        f"and the patient reader keeps turning pages to see where the story goes next.</p>"
        for n in range(1, count + 1)
    )


def chapter(title, wrapper_id, anchor_id):
    return xhtml(
        title,
        f"""<div class="chapter" id="{wrapper_id}">
  <h2><a id="{anchor_id}"></a>{title}</h2>
{paragraphs(title, 120)}
</div>""",
    )


def xhtml(title, body):
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>{title}</title></head>
<body>
{body}
</body>
</html>
"""


files = {
    "META-INF/container.xml": """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>
""",
    "OEBPS/content.opf": """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="pub-id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>Long Chapter Fixture</dc:title>
    <dc:identifier id="pub-id">long-chapter-before-target-fixture</dc:identifier>
    <dc:language>en</dc:language>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="cover" href="cover.xhtml" media-type="application/xhtml+xml"/>
    <item id="contents" href="contents.xhtml" media-type="application/xhtml+xml"/>
    <item id="c1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
    <item id="c2" href="chapter2.xhtml" media-type="application/xhtml+xml"/>
    <item id="end" href="endmatter.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine>
    <itemref idref="cover"/>
    <itemref idref="contents"/>
    <itemref idref="c1"/>
    <itemref idref="c2"/>
    <itemref idref="end"/>
  </spine>
</package>
""",
    "OEBPS/nav.xhtml": """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<head><title>Navigation</title></head>
<body>
<nav epub:type="toc" id="toc">
  <ol>
    <li><a href="contents.xhtml#contents">Contents</a></li>
    <li><a href="chapter1.xhtml#chapter-one">Chapter One</a></li>
    <li><a href="chapter2.xhtml#chapter-two">Chapter Two</a></li>
    <li><a href="endmatter.xhtml#title-page">The Fixture Title</a></li>
    <li><a href="endmatter.xhtml#title-page">Detailed Contents</a></li>
    <li><a href="endmatter.xhtml#licence">The Full Licence</a>
      <ol>
        <li><a href="endmatter.xhtml#licence-section-one">Licence Section One</a></li>
      </ol>
    </li>
  </ol>
</nav>
</body>
</html>
""",
    "OEBPS/cover.xhtml": xhtml(
        "Cover",
        """<div class="cover">
  <h1>Long Chapter Fixture</h1>
  <p>A cover page for the long chapter fixture.</p>
</div>""",
    ),
    "OEBPS/contents.xhtml": xhtml(
        "Contents",
        """<div class="chapter" id="contents">
  <h2>Contents</h2>
  <p><a href="chapter1.xhtml#chap01">Go to Chapter One</a></p>
  <p><a href="chapter2.xhtml#chap02">Go to Chapter Two</a></p>
</div>""",
    ),
    "OEBPS/chapter1.xhtml": chapter("Chapter One", "chapter-one", "chap01"),
    "OEBPS/chapter2.xhtml": chapter("Chapter Two", "chapter-two", "chap02"),
    "OEBPS/endmatter.xhtml": xhtml(
        "End Matter",
        f"""<div class="chapter" id="title-page">
  <h1>The Fixture Title</h1>
  <h2>Detailed Contents</h2>
{paragraphs("Detailed Contents", 3)}
</div>
<div class="chapter" id="licence">
  <h2>The Full Licence</h2>
  <div id="licence-section-one">
  <h3>Licence Section One</h3>
{paragraphs("Licence Section One", 120)}
  </div>
</div>""",
    ),
}

with zipfile.ZipFile(out, "w") as z:
    z.writestr(zipfile.ZipInfo("mimetype", STAMP), "application/epub+zip", zipfile.ZIP_STORED)
    for name, content in files.items():
        z.writestr(zipfile.ZipInfo(name, STAMP), content, zipfile.ZIP_DEFLATED)

print(f"wrote {out}")
PY
