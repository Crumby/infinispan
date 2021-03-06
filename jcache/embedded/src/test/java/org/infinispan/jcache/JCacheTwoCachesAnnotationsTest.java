package org.infinispan.jcache;

import static org.infinispan.test.AbstractCacheTest.getDefaultClusteredCacheConfig;

import java.lang.reflect.Method;
import java.net.URI;
import javax.cache.Cache;
import javax.inject.Inject;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.jcache.annotation.InjectedCachePutInterceptor;
import org.infinispan.jcache.embedded.JCache;
import org.infinispan.jcache.embedded.JCacheManager;
import org.infinispan.jcache.util.JCacheTestingUtil;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Matej Cimbora
 */
@Test(testName = "org.infinispan.jcache.JCacheTwoCachesAnnotationsTest", groups = "functional")
public class JCacheTwoCachesAnnotationsTest extends AbstractTwoCachesAnnotationsTest {

   private EmbeddedCacheManager cacheManager1;
   private EmbeddedCacheManager cacheManager2;

   @Deployment
   public static JavaArchive createDeployment() {
      return ShrinkWrap.create(JavaArchive.class).addPackage(JCacheAnnotatedClass.class.getPackage()).addPackage(JCache.class.getPackage())
            .addPackage(InjectedCachePutInterceptor.class.getPackage()).addPackage(CacheProducer.class.getPackage()).addPackage(JCacheTestingUtil.class.getPackage())
            .addAsResource(JCacheTwoCachesAnnotationsTest.class.getResource("/beans.xml"), "beans.xml");
   }

   @Inject
   private JCacheAnnotatedClass jCacheAnnotatedClass;

   @Override
   public JCacheAnnotatedClass getJCacheAnnotatedClass() {
      return jCacheAnnotatedClass;
   }

   @Override
   public Cache getCache1(Method m) {
      JCacheManager jCacheManager = new JCacheManager(URI.create(m.getName()), cacheManager1, null);
      return jCacheManager.getCache("annotation");
   }

   @Override
   public Cache getCache2(Method m) {
      JCacheManager jCacheManager = new JCacheManager(URI.create(m.getName()), cacheManager2, null);
      return jCacheManager.getCache("annotation");
   }

   @BeforeClass(alwaysRun = true)
   public void initCacheManagers() {
      cacheManager1 = TestCacheManagerFactory.createClusteredCacheManager(JCacheTestSCI.INSTANCE, getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC));
      cacheManager1.defineConfiguration("annotation", getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC).build());
      cacheManager2 = TestCacheManagerFactory.createClusteredCacheManager(JCacheTestSCI.INSTANCE, getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC));
      cacheManager2.defineConfiguration("annotation", getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC).build());
      TestingUtil.blockUntilViewsReceived(30000,
            cacheManager1.getCache("annotation"),
            cacheManager2.getCache("annotation"));
   }

   @AfterMethod
   public void cleanCaches() {
      TestingUtil.clearContent(cacheManager1, cacheManager2);
   }

   @AfterClass(alwaysRun = true)
   public void killCacheManagers() {
      TestingUtil.killCacheManagers(cacheManager1, cacheManager2);
   }
}
