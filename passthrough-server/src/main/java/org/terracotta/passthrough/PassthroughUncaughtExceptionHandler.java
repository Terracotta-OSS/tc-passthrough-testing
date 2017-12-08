/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.UncaughtExceptionHandler;


/**
 * The uncaught exception handler installed for all the threads in the passthrough testing system.  All it does is log the
 * error and terminate the VM.
 */
public final class PassthroughUncaughtExceptionHandler implements UncaughtExceptionHandler {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public static final PassthroughUncaughtExceptionHandler sharedInstance = new PassthroughUncaughtExceptionHandler();

  private PassthroughUncaughtExceptionHandler() {}

  @Override
  public void uncaughtException(Thread thread, Throwable error) {
    logger.error("FATAL EXCEPTION IN PASSTHROUGH THREAD", error);
    System.exit(1);
  }
}
