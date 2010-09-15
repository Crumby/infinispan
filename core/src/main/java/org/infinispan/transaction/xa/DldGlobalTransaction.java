package org.infinispan.transaction.xa;

import org.infinispan.marshall.Ids;
import org.infinispan.marshall.Marshallable;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

/**
 * This class is used when deadlock detection is enabled.
 *
 * @author Mircea.Markus@jboss.com
 */
@Marshallable(externalizer = DldGlobalTransaction.Externalizer.class, id = Ids.DEADLOCK_DETECTING_GLOBAL_TRANSACTION)
public class DldGlobalTransaction extends GlobalTransaction {

   private static Log log = LogFactory.getLog(DldGlobalTransaction.class);

   public static final boolean trace = log.isTraceEnabled();

   private volatile long coinToss;

   private volatile boolean isMarkedForRollback;

   private transient volatile Object localLockIntention;

   protected volatile Set<Object> remoteLockIntention = Collections.EMPTY_SET;

   private volatile Set<Object> locksAtOrigin = Collections.EMPTY_SET;

   public DldGlobalTransaction() {
   }

   DldGlobalTransaction(Address addr, boolean remote) {
      super(addr, remote);
   }


   /**
    * Sets the reandom number that defines the coin toss.
    */
   public void setCoinToss(long coinToss) {
      this.coinToss = coinToss;
   }

   public long getCoinToss() {
      return coinToss;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof DldGlobalTransaction)) return false;
      if (!super.equals(o)) return false;

      DldGlobalTransaction that = (DldGlobalTransaction) o;

      if (coinToss != that.coinToss) return false;
      return true;
   }

   @Override
   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (int) (coinToss ^ (coinToss >>> 32));
      return result;
   }

   @Override
   public String toString() {
      return "DldGlobalTransaction{" +
            "coinToss=" + coinToss +
            ", isMarkedForRollback=" + isMarkedForRollback +
            ", lockIntention=" + localLockIntention +
            ", affectedKeys=" + remoteLockIntention +
            ", locksAtOrigin=" + locksAtOrigin +
            "} " + super.toString();
   }

   public synchronized boolean isMarkedForRollback() {
      return isMarkedForRollback;
   }

   public synchronized void setMarkedForRollback(boolean markedForRollback) {
      isMarkedForRollback = markedForRollback;
   }

   /**
    * Returns the key this transaction intends to lock. 
    */
   public Object getLockIntention() {
      return localLockIntention;
   }

   public void setLockIntention(Object lockIntention) {                                                                                                       
      this.localLockIntention = lockIntention;
   }

   public boolean wouldLose(DldGlobalTransaction other) {
      return this.coinToss < other.coinToss;
   }

   public boolean isAcquiringRemoteLock(Object key, Address address) {
      boolean contains = remoteLockIntention.contains(key);
      if (trace) log.trace("Intention check: does " + remoteLockIntention + " contain " + key + "? " + contains);
      return contains; //this works for replication
   }

   public void setRemoteLockIntention(Set<Object> remoteLockIntention) {
      if (trace) {
         log.trace("Setting the remote lock intention: " + remoteLockIntention);
      }
      this.remoteLockIntention = remoteLockIntention;
   }

   public Set<Object> getRemoteLockIntention() {
      return remoteLockIntention;
   }

   public boolean hasLockAtOrigin(Set<Object> remoteLockIntention) {
      if (log.isTraceEnabled()) log.trace("Our(" + this + ") locks at origin are: " + locksAtOrigin + ". Others remote lock intention is: " + remoteLockIntention);
      for (Object key : remoteLockIntention) {
         if (this.locksAtOrigin.contains(key)) {
            return true;
         }
      }
      return false;
   }

   public void setLocksHeldAtOrigin(Set<Object> locksAtOrigin) {
      if (trace) log.trace("Setting locks at origin for (" + this + ")  to " + locksAtOrigin);
      this.locksAtOrigin = locksAtOrigin;
   }

   public Set<Object> getLocksHeldAtOrigin() {
      return this.locksAtOrigin;
   }

   public static class Externalizer extends GlobalTransaction.Externalizer {
      public Externalizer() {
         gtxFactory = new GlobalTransactionFactory(true);
      }

      @Override
      public void writeObject(ObjectOutput output, Object subject) throws IOException {
         super.writeObject(output, subject);
         DldGlobalTransaction ddGt = (DldGlobalTransaction) subject;
         output.writeLong(ddGt.getCoinToss());
         if (ddGt.locksAtOrigin.isEmpty()) {
            output.writeObject(null);
         } else {
            output.writeObject(ddGt.locksAtOrigin);
         }
      }

      @Override
      public Object readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         DldGlobalTransaction ddGt = (DldGlobalTransaction) super.readObject(input);
         ddGt.setCoinToss(input.readLong());
         Object locksAtOriginObj = input.readObject();
         if (locksAtOriginObj == null) {
            ddGt.setLocksHeldAtOrigin(Collections.EMPTY_SET);
         } else {
            ddGt.setLocksHeldAtOrigin((Set<Object>) locksAtOriginObj);
         }
         return ddGt;
      }
   }
}
