package io.cratekube.clustermgmt.service

import io.cratekube.clustermgmt.api.ProcessExecutor
import io.cratekube.clustermgmt.api.DefaultProcessExecutor

/**
 * Executor for the {@code kubectl} binary.
 *
 * @see ProcessExecutor
 */
class KubectlCommand extends DefaultProcessExecutor {
  String executablePath = '/bin/kubectl'
}
