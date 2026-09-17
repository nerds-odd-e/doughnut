package com.odde.donut.controllers;

/**
 * Typed-note bodies calibrated against the configured 50% JGit rename-similarity threshold, for
 * proposal tests that need a detected rename or an unresolved removal/addition mixture.
 */
final class NotebookGitRenameScoringBodies {
  private NotebookGitRenameScoringBodies() {}

  /**
   * Substantial typed-note body used as the original baseline for JGit rename scoring. Multiple
   * non-trivial prose lines ensure shared frontmatter cannot dominate the similarity score.
   */
  static final String SUBSTANTIAL_ORIGINAL_BODY =
      "---\ntype: Note\n---\n"
          + "The quick brown fox jumps over the lazy dog near the riverbank.\n"
          + "She decided to read the ancient manuscript that described the valley.\n"
          + "Mountains rose in the distance, their peaks covered with fresh snow.\n"
          + "A small village nestled between them kept its traditions alive for generations.\n"
          + "Travelers came each spring to trade cloth and spices at the market.\n"
          + "Children played near the fountain while elders discussed the harvest.\n"
          + "The librarian organized every scroll by region and by season.\n";

  /**
   * Substantial typed-note body topically unrelated to {@link #SUBSTANTIAL_ORIGINAL_BODY}; their
   * JGit similarity is well below the configured 50% threshold, so a removal/addition pair using
   * these two bodies stays an unresolved mixture rather than a detected rename.
   */
  static final String SUBSTANTIAL_UNRELATED_BODY =
      "---\ntype: Note\n---\n"
          + "Quantum entanglement links particles across vast distances instantly.\n"
          + "Researchers measured photon spins in supercooled vacuum chambers.\n"
          + "The experiment required precision instruments and calm steady hands.\n"
          + "Equations described probability amplitudes rather than certainties.\n"
          + "Funding agencies reviewed the proposal for six months before approving.\n"
          + "Graduate students calibrated lasers late into the quiet night.\n"
          + "A paper summarizing findings was submitted to a prominent journal.\n";

  /**
   * Substantial typed-note body edited from {@link #SUBSTANTIAL_ORIGINAL_BODY} so the JGit
   * similarity score lands in [50, 60): distinguishes the configured 50% rename policy from JGit's
   * 60% default while genuinely changing content. Shared by inferred move-and-edit reference
   * fixtures across rename and relocation referrer controller tests.
   */
  static final String MODERATE_EDIT_BODY =
      "---\ntype: Note\n---\n"
          + "The quick brown fox jumps over the lazy dog near the riverbank.\n"
          + "She decided to read the ancient manuscript that described the valley.\n"
          + "Mountains rose in the distance, their peaks covered with fresh snow.\n"
          + "A small village nestled between them kept its traditions alive for generations.\n"
          + "Completely rewritten prose about oceans and sailing ships replaces this.\n"
          + "Sailors navigated by stars across the wide and stormy open waters.\n"
          + "The harbor master logged each vessel and collected the docking fees.\n";
}
