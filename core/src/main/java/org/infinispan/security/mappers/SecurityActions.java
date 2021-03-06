package org.infinispan.security.mappers;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.security.Security;
import org.infinispan.security.actions.GetCacheManagerConfigurationAction;
import org.infinispan.security.actions.GetGlobalComponentRegistryAction;

/**
 * SecurityActions for the org.infinispan.security.mappers package.
 *
 * Do not move. Do not change class and method visibility to avoid being called from other
 * {@link java.security.CodeSource}s, thus granting privilege escalation to external code.
 *
 * @author Tristan Tarrant
 * @since 10.1
 */
final class SecurityActions {
   private static <T> T doPrivileged(PrivilegedAction<T> action) {
      if (System.getSecurityManager() != null) {
         return AccessController.doPrivileged(action);
      } else {
         return Security.doPrivileged(action);
      }
   }

   static GlobalComponentRegistry getGlobalComponentRegistry(EmbeddedCacheManager cacheManager) {
      return doPrivileged(new GetGlobalComponentRegistryAction(cacheManager));
   }

   static GlobalConfiguration getCacheManagerConfiguration(EmbeddedCacheManager cacheManager) {
      return doPrivileged(new GetCacheManagerConfigurationAction(cacheManager));
   }
}
