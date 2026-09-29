package com.odde.donut.services.book;

/**
 * One table-of-contents entry of an EPUB, from its navigation document or, without one, from spine
 * headings. {@code startPreorderIfNoFragment} locates a heading that has no {@code id}.
 */
record EpubTocEntry(
    String title,
    int depth,
    String spineZipPath,
    String fragmentId,
    Integer startPreorderIfNoFragment) {}
