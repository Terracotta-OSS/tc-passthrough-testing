package org.terracotta.passthrough;

import org.terracotta.entity.EntityUserException;
import org.terracotta.exception.EntityException;

public class PassthroughEntityUserExceptionWrapper extends EntityException {
  public PassthroughEntityUserExceptionWrapper(final EntityUserException cause) {
    super(null, null, cause.getMessage(), cause);
  }
}
