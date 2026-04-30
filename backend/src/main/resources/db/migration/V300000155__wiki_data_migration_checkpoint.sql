-- Holds in-progress wiki data migration checkpoints so batch progress survives restarts.
-- Deleted when migration completes successfully. Rows in wiki_data_migration_slug_notebook_done
-- record notebooks whose slug trees were rebuilt in SLUG_NOTEBOOKS.

CREATE TABLE wiki_data_migration_checkpoint (
  id TINYINT NOT NULL PRIMARY KEY,
  phase VARCHAR(32) NOT NULL,
  topo_pairs_done INT NOT NULL DEFAULT 0,
  topology_pair_total INT NOT NULL DEFAULT 0,
  slug_prep_done TINYINT(1) NOT NULL DEFAULT 0,
  detached_child_folders INT NOT NULL DEFAULT 0,
  updated_normal_notes INT NOT NULL DEFAULT 0,
  updated_relation_notes INT NOT NULL DEFAULT 0,
  deleted_obsolete_roots INT NOT NULL DEFAULT 0,
  batches_completed INT NOT NULL DEFAULT 0,
  batch_total_planned INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE wiki_data_migration_slug_notebook_done (
  notebook_id INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
