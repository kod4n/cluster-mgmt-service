package io.cratekube.clustermgmt.service

import io.cratekube.clustermgmt.api.ProcessExecutor
import io.cratekube.clustermgmt.api.DefaultProcessExecutor

/**
 * Executor for the {@code rke} binary.
 *
 * @see ProcessExecutor
 */
class RkeCommand extends DefaultProcessExecutor {
  String executablePath = '/bin/rke'
}
