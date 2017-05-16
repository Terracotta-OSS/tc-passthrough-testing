package org.terracotta.passthrough;

import org.terracotta.exception.EntityException;
import org.terracotta.exception.RuntimeEntityException;

public class PassthroughRuntimeEntityExceptionWrapper extends EntityException {
  public PassthroughRuntimeEntityExceptionWrapper(RuntimeEntityException exception) {
    super(exception.getClassName(), exception.getEntityName(), exception.getDescription(), exception);
  }
}
