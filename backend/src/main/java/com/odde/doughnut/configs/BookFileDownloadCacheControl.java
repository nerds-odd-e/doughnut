package com.odde.doughnut.configs;

import java.util.concurrent.TimeUnit;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Component;

@Component
public class BookFileDownloadCacheControl {

  private final CacheControl cacheControl;

  public BookFileDownloadCacheControl(Environment env) {
    if (env.acceptsProfiles(Profiles.of("prod"))) {
      this.cacheControl = CacheControl.maxAge(365, TimeUnit.DAYS).cachePrivate().mustRevalidate();
    } else {
      this.cacheControl = CacheControl.noStore();
    }
  }

  public CacheControl value() {
    return cacheControl;
  }
}
