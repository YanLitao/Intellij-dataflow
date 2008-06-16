package com.intellij.openapi.vcs.changes.committed;

public enum CommittedChangesBrowserUseCase {
  COMMITTED,
  INCOMING,
  UPDATE,
  IN_AIR;

  public final static String CONTEXT_NAME = "COMMITTED_CHANGES_BROWSER_USE_CASE";
}
