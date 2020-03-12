package io.cratekube.clustermgmt

import io.cratekube.clustermgmt.modules.IntegrationSpecModule
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.hook.GuiceyConfigurationHook

/**
 * Hook used to modify Guice modules for integration specs.
 */
class IntegrationSpecHook implements GuiceyConfigurationHook {
  @Override
  void configure(GuiceBundle.Builder builder) {
    builder.modulesOverride(new IntegrationSpecModule())
  }
}
