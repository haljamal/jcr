/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.lock.cacheable;

import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.services.jcr.config.LockManagerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.ChangesLogIterator;
import org.exoplatform.services.jcr.dataflow.CompositeChangesLog;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.lock.LockRemover;
import org.exoplatform.services.jcr.impl.core.lock.LockRemoverHolder;
import org.exoplatform.services.jcr.impl.core.lock.SessionLockManager;
import org.exoplatform.services.jcr.impl.dataflow.TransientItemData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;
import org.picocontainer.Startable;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: AbstractCacheableLockManagerImpl.java 2806 2010-07-21 08:00:15Z tolusha $
 */
public abstract class AbstractCacheableLockManager implements CacheableLockManager, ItemsPersistenceListener, Startable
{
   /**
    *  The name to property time out.  
    */
   public static final String TIME_OUT = "time-out";

   /**
    * Default lock time out. 30min
    */
   public static final long DEFAULT_LOCK_TIMEOUT = 1000 * 60 * 30;

   /**
    * Data manager.
    */
   protected final DataManager dataManager;

   /**
    * Run time lock time out.
    */
   protected long lockTimeOut;

   /**
    * Lock remover thread.
    */
   protected LockRemover lockRemover;

   /**
    * SessionLockManagers that uses this LockManager.
    */
   protected Map<String, CacheableSessionLockManager> sessionLockManagers;

   /**
    * The current Transaction Manager
    */
   protected TransactionManager tm;

   /**
    * Logger
    */
   private final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.AbstractCacheableLockManager");

   protected LockActionNonTxAware<Integer, Object> getNumLocks;

   protected LockActionNonTxAware<Boolean, Object> hasLocks;

   protected LockActionNonTxAware<Boolean, String> isLockLive;

   protected LockActionNonTxAware<Object, LockData> refresh;

   protected LockActionNonTxAware<Boolean, String> lockExist;

   protected LockActionNonTxAware<LockData, String> getLockDataById;

   protected LockActionNonTxAware<List<LockData>, Object> getLockList;

   public static final String JDBC_TABLE_NAME_SUFFIX = "jdbc.table.name";

   /**
    * Constructor.
    * 
    * @param dataManager - workspace persistent data manager
    * @param config - workspace entry
    * @param transactionManager 
    *          the transaction manager
    * @throws RepositoryConfigurationException 
    */
   public AbstractCacheableLockManager(WorkspacePersistentDataManager dataManager, WorkspaceEntry config,
      TransactionManager transactionManager, LockRemoverHolder lockRemoverHolder)
      throws RepositoryConfigurationException
   {
      if (config.getLockManager() != null)
      {
         if (config.getLockManager().getParameters() != null
            && config.getLockManager().getParameterValue(TIME_OUT, null) != null)
         {
            long timeOut = config.getLockManager().getParameterTime(TIME_OUT);
            lockTimeOut = timeOut > 0 ? timeOut : DEFAULT_LOCK_TIMEOUT;
         }
         else
         {
            lockTimeOut =
               config.getLockManager().getTimeout() > 0 ? config.getLockManager().getTimeout() : DEFAULT_LOCK_TIMEOUT;
         }
      }
      else
      {
         lockTimeOut = DEFAULT_LOCK_TIMEOUT;
      }

      this.dataManager = dataManager;
      this.sessionLockManagers = new ConcurrentHashMap<String, CacheableSessionLockManager>();
      this.tm = transactionManager;
      this.lockRemover = lockRemoverHolder.getLockRemover(this);
      dataManager.addItemPersistenceListener(this);
   }

   @Managed
   @ManagedDescription("Remove the expired locks")
   public void cleanExpiredLocks()
   {
      removeExpired();
   }

   public long getDefaultLockTimeOut()
   {
      return lockTimeOut;
   }

   @Managed
   @ManagedDescription("The number of active locks")
   public int getNumLocks()
   {
      try
      {
         return executeLockActionNonTxAware(getNumLocks, null);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return -1;
   }

   /**
    * Indicates if some locks have already been created
    */
   protected boolean hasLocks()
   {
      try
      {
         return executeLockActionNonTxAware(hasLocks, null);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return true;
   }

   /**
    * Return new instance of session lock manager.
    */
   public SessionLockManager getSessionLockManager(String sessionId, SessionDataManager transientManager)
   {
      CacheableSessionLockManager sessionManager = new CacheableSessionLockManager(sessionId, this, transientManager);
      sessionLockManagers.put(sessionId, sessionManager);
      return sessionManager;
   }

   /**
    * Check is LockManager contains lock. No matter it is in pending or persistent state.
    * 
    * @param nodeId - locked node id
    * @return 
    */
   public boolean isLockLive(String nodeId) throws LockException
   {
      try
      {
         return executeLockActionNonTxAware(isLockLive, nodeId);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void onSaveItems(ItemStateChangesLog changesLog)
   {
      List<PlainChangesLog> chengesLogList = new ArrayList<PlainChangesLog>();
      if (changesLog instanceof TransactionChangesLog)
      {
         ChangesLogIterator logIterator = ((TransactionChangesLog)changesLog).getLogIterator();

         while (logIterator.hasNextLog())
         {
            chengesLogList.add(logIterator.nextLog());
         }
      }
      else if (changesLog instanceof PlainChangesLog)
      {
         chengesLogList.add((PlainChangesLog)changesLog);
      }
      else if (changesLog instanceof CompositeChangesLog)
      {
         for (ChangesLogIterator iter = ((CompositeChangesLog)changesLog).getLogIterator(); iter.hasNextLog();)
         {
            chengesLogList.add(iter.nextLog());
         }
      }

      List<LockOperationContainer> containers = new ArrayList<LockOperationContainer>();

      for (PlainChangesLog currChangesLog : chengesLogList)
      {
         String sessionId = currChangesLog.getSessionId();

         String nodeIdentifier;
         try
         {
            switch (currChangesLog.getEventType())
            {
               case ExtendedEvent.LOCK :
                  if (currChangesLog.getSize() < 2)
                  {
                     LOG.error("Incorrect changes log  of type ExtendedEvent.LOCK size=" + currChangesLog.getSize()
                        + "<2 \n" + currChangesLog.dump());
                     break;
                  }
                  nodeIdentifier = currChangesLog.getAllStates().get(0).getData().getParentIdentifier();

                  CacheableSessionLockManager session = sessionLockManagers.get(sessionId);
                  if (session != null && session.containsPendingLock(nodeIdentifier))
                  {
                     containers.add(new LockOperationContainer(nodeIdentifier, currChangesLog.getSessionId(),
                        ExtendedEvent.LOCK));
                  }
                  else
                  {
                     LOG.error("Lock must exist in pending locks.");
                  }
                  break;
               case ExtendedEvent.UNLOCK :
                  if (currChangesLog.getSize() < 2)
                  {
                     LOG.error("Incorrect changes log  of type ExtendedEvent.UNLOCK size=" + currChangesLog.getSize()
                        + "<2 \n" + currChangesLog.dump());
                     break;
                  }

                  containers.add(new LockOperationContainer(currChangesLog.getAllStates().get(0).getData()
                     .getParentIdentifier(), currChangesLog.getSessionId(), ExtendedEvent.UNLOCK));
                  break;
               default :
                  HashSet<String> removedLock = new HashSet<String>();
                  for (ItemState itemState : currChangesLog.getAllStates())
                  {
                     // this is a node and node is locked
                     if (itemState.getData().isNode() && lockExist(itemState.getData().getIdentifier()))
                     {
                        nodeIdentifier = itemState.getData().getIdentifier();
                        if (itemState.isDeleted())
                        {
                           removedLock.add(nodeIdentifier);
                        }
                        else if (itemState.isAdded() || itemState.isRenamed() || itemState.isUpdated())
                        {
                           removedLock.remove(nodeIdentifier);
                        }
                     }
                  }
                  for (String identifier : removedLock)
                  {
                     containers.add(new LockOperationContainer(identifier, currChangesLog.getSessionId(),
                        ExtendedEvent.UNLOCK));
                  }
                  break;
            }
         }
         catch (IllegalStateException e)
         {
            LOG.error(e.getLocalizedMessage(), e);
         }
      }

      // sort locking and unlocking operations to avoid deadlocks
      Collections.sort(containers);
      for (LockOperationContainer container : containers)
      {
         try
         {
            container.apply();
         }
         catch (LockException e)
         {
            LOG.error(e.getMessage(), e);
         }
      }
   }

   /**
    * Class containing operation type (LOCK or UNLOCK) and all the needed information like node uuid and session id.
    */
   private class LockOperationContainer implements Comparable<LockOperationContainer>
   {

      private String identifier;

      private String sessionId;

      private int type;

      /**
       * @param identifier node identifier 
       * @param sessionId id of session
       * @param type ExtendedEvent type specifying the operation (LOCK or UNLOCK)
       */
      public LockOperationContainer(String identifier, String sessionId, int type)
      {
         super();
         this.identifier = identifier;
         this.sessionId = sessionId;
         this.type = type;
      }

      /**
       * @return node identifier
       */
      public String getIdentifier()
      {
         return identifier;
      }

      public void apply() throws LockException
      {
         // invoke internalLock in LOCK operation
         if (type == ExtendedEvent.LOCK)
         {
            internalLock(sessionId, identifier);
         }
         // invoke internalUnLock in UNLOCK operation
         else if (type == ExtendedEvent.UNLOCK)
         {
            internalUnLock(sessionId, identifier);
         }
      }

      /**
       * @see java.lang.Comparable#compareTo(java.lang.Object)
       */
      public int compareTo(LockOperationContainer o)
      {
         return identifier.compareTo(o.getIdentifier());
      }
   }

   /**
    * Refreshed lock data in cache
    * 
    * @param newLockData
    */
   public void refreshLockData(LockData newLockData) throws LockException
   {
      executeLockActionNonTxAware(refresh, newLockData);
   }

   /**
    * Remove expired locks. Used from LockRemover.
    */
   public synchronized void removeExpired()
   {
      final List<String> removeLockList = new ArrayList<String>();

      for (LockData lock : getLockList())
      {
         if (!lock.isSessionScoped() && lock.getTimeToDeath() < 0)
         {
            removeLockList.add(lock.getNodeIdentifier());
         }
      }

      Collections.sort(removeLockList);

      for (String rLock : removeLockList)
      {
         removeLock(rLock);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      lockRemover.start();
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      lockRemover.stop();
      sessionLockManagers.clear();
   }

   /**
    * Copy <code>PropertyData prop<code> to new TransientItemData
    * 
    * @param prop
    * @return
    * @throws RepositoryException
    */
   protected TransientItemData copyItemData(PropertyData prop) throws RepositoryException
   {
      if (prop == null)
      {
         return null;
      }

      // make a copy, value may be null for deleting items
      TransientPropertyData newData =
         new TransientPropertyData(prop.getQPath(), prop.getIdentifier(), prop.getPersistedVersion(), prop.getType(),
            prop.getParentIdentifier(), prop.isMultiValued(), prop.getValues());

      return newData;
   }

   /**
    * Internal lock
    * 
    * @param nodeIdentifier
    * @throws LockException
    */
   protected abstract void internalLock(String sessionId, String nodeIdentifier) throws LockException;

   /**
    * Internal unlock.
    * 
    * @param sessionId
    * @param nodeIdentifier
    * @throws LockException
    */
   protected abstract void internalUnLock(String sessionId, String nodeIdentifier) throws LockException;

   /**
    * {@inheritDoc}
    */
   public boolean lockExist(String nodeId)
   {
      try
      {
         return executeLockActionNonTxAware(lockExist, nodeId);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public String getLockTokenHash(String token)
   {
      String hash = "";
      try
      {
         MessageDigest m = MessageDigest.getInstance("MD5");
         m.update(token.getBytes(), 0, token.length());
         hash = new BigInteger(1, m.digest()).toString(16);
      }
      catch (NoSuchAlgorithmException e)
      {
         LOG.error("Can't get instanse of MD5 MessageDigest!", e);
      }
      return hash;
   }

   /**
    * {@inheritDoc}
    */
   public LockData getExactNodeOrCloseParentLock(NodeData node) throws RepositoryException
   {
      return getExactNodeOrCloseParentLock(node, true);
   }

   private LockData getExactNodeOrCloseParentLock(NodeData node, boolean checkHasLocks) throws RepositoryException
   {

      if (node == null || (checkHasLocks && !hasLocks()))
      {
         return null;
      }
      LockData retval = null;
      retval = getLockDataById(node.getIdentifier());
      if (retval == null)
      {
         NodeData parentData = (NodeData)dataManager.getItemData(node.getParentIdentifier());
         if (parentData != null)
         {
            retval = getExactNodeOrCloseParentLock(parentData, false);
         }
      }
      return retval;
   }

   /**
    * {@inheritDoc}
    */
   public LockData getExactNodeLock(NodeData node) throws RepositoryException
   {
      if (node == null || !hasLocks())
      {
         return null;
      }

      return getLockDataById(node.getIdentifier());
   }

   /**
    * {@inheritDoc}
    */
   public LockData getClosedChild(NodeData node) throws RepositoryException
   {
      return getClosedChild(node, true);
   }

   private LockData getClosedChild(NodeData node, boolean checkHasLocks) throws RepositoryException
   {

      if (node == null || (checkHasLocks && !hasLocks()))
      {
         return null;
      }
      LockData retval = null;

      List<NodeData> childData = dataManager.getChildNodesData(node);
      for (NodeData nodeData : childData)
      {
         retval = getLockDataById(nodeData.getIdentifier());
         if (retval != null)
         {
            return retval;
         }
      }
      // child not found try to find dipper
      for (NodeData nodeData : childData)
      {
         retval = getClosedChild(nodeData, false);
         if (retval != null)
         {
            return retval;
         }
      }
      return retval;
   }

   protected LockData getLockDataById(String nodeId)
   {
      try
      {
         return executeLockActionNonTxAware(getLockDataById, nodeId);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return null;
   }

   protected synchronized List<LockData> getLockList()
   {
      try
      {
         return executeLockActionNonTxAware(getLockList, null);
      }
      catch (LockException e)
      {
         // ignore me will never occur
      }
      return null;
   }

   /**
    * Remove lock, used by Lock remover.
    * 
    * @param nodeIdentifier String
    */
   protected void removeLock(String nodeIdentifier)
   {
      try
      {
         NodeData nData = (NodeData)dataManager.getItemData(nodeIdentifier);

         //TODO EXOJCR-412, should be refactored in future.
         //Skip removing, because that node was removed in other node of cluster.  
         if (nData == null)
         {
            return;
         }

         PlainChangesLog changesLog =
            new PlainChangesLogImpl(new ArrayList<ItemState>(), IdentityConstants.SYSTEM, ExtendedEvent.UNLOCK);

         ItemData lockOwner =
            copyItemData((PropertyData)dataManager.getItemData(nData, new QPathEntry(Constants.JCR_LOCKOWNER, 1),
               ItemType.PROPERTY));

         //TODO EXOJCR-412, should be refactored in future.
         //Skip removing, because that lock was removed in other node of cluster.  
         if (lockOwner == null)
         {
            return;
         }

         changesLog.add(ItemState.createDeletedState(lockOwner));

         ItemData lockIsDeep =
            copyItemData((PropertyData)dataManager.getItemData(nData, new QPathEntry(Constants.JCR_LOCKISDEEP, 1),
               ItemType.PROPERTY));

         //TODO EXOJCR-412, should be refactored in future.
         //Skip removing, because that lock was removed in other node of cluster.  
         if (lockIsDeep == null)
         {
            return;
         }

         changesLog.add(ItemState.createDeletedState(lockIsDeep));

         // lock probably removed by other thread
         if (lockOwner == null && lockIsDeep == null)
         {
            return;
         }

         dataManager.save(new TransactionChangesLog(changesLog));
      }
      catch (JCRInvalidItemStateException e)
      {
         //TODO EXOJCR-412, should be refactored in future.
         //Skip property not found in DB, because that lock property was removed in other node of cluster.
         if (LOG.isDebugEnabled())
         {
            LOG.debug("The propperty was removed in other node of cluster.", e);
         }

      }
      catch (RepositoryException e)
      {
         LOG.error("Error occur during removing lock" + e.getLocalizedMessage(), e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void closeSessionLockManager(String sessionID)
   {
      sessionLockManagers.remove(sessionID);
   }

   /**
    * Execute the given action outside a transaction. This is needed since the {@link Cache} used by implementation of {@link CacheableLockManager}
    * to manage the persistence of its locks thanks to a {@link CacheLoader} and a {@link CacheLoader} lock the cache {@link Node}
    * even for read operations which cause deadlock issue when a XA {@link Transaction} is already opened
    * @throws LockException when a exception occurs
    */
   private <R, A> R executeLockActionNonTxAware(LockActionNonTxAware<R, A> action, A arg) throws LockException
   {
      Transaction tx = null;
      try
      {
         if (tm != null)
         {
            try
            {
               tx = tm.suspend();
            }
            catch (Exception e)
            {
               LOG.warn("Cannot suspend the current transaction", e);
            }
         }
         return action.execute(arg);
      }
      finally
      {
         if (tx != null)
         {
            try
            {
               tm.resume(tx);
            }
            catch (Exception e)
            {
               LOG.warn("Cannot resume the current transaction", e);
            }
         }
      }
   }

   /**
    * Actions that are not supposed to be called within a transaction
    * 
    * Created by The eXo Platform SAS
    * Author : Nicolas Filotto 
    *          nicolas.filotto@exoplatform.com
    * 21 janv. 2010
    */
   protected static interface LockActionNonTxAware<R, A>
   {
      R execute(A arg) throws LockException;
   }

   /**
    * Return table name for lock data.
    */
   public static String getLockTableName(LockManagerEntry lockManagerEntry)
   {
      if (lockManagerEntry != null)
      {
         for (SimpleParameterEntry entry : lockManagerEntry.getParameters())
         {
            if (entry.getName().contains(AbstractCacheableLockManager.JDBC_TABLE_NAME_SUFFIX))
            {
               return entry.getValue();
            }
         }
      }

      return null;
   }
}
