UPDATE `memory_tracker`
  SET `difficulty` = 5
  WHERE `difficulty` IS NULL
    AND (`stability` > 0 OR `recall_count` > 0);
