/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class ReadOnlyPooledSession implements Session{

    private static final Logger log = LoggerFactory.getLogger(ReadOnlyPooledSession.class);
    
    private Session delegatee;
    private long creationTime;
    private int lastRefreshDivMinute;
    private JcrSessionPool jcrSessionPool;
    private static final int TIME_TO_LIVE_SECONDS = 3600;
    private static final int REFRESH_MINUTE_INTERVAL = 3;
    
    private int refresh_interval_minutes = REFRESH_MINUTE_INTERVAL;
    private int refCount;
    private long lastRefresTick;
    private long idleSince;
    private boolean dirty;
    
    public ReadOnlyPooledSession(Session session, JcrSessionPool  jcrSessionPool) {
        this.delegatee = session;
        this.creationTime = System.currentTimeMillis();
        this.lastRefresTick = creationTime;
        this.lastRefreshDivMinute = Calendar.getInstance().get(Calendar.MINUTE)/REFRESH_MINUTE_INTERVAL;
        this.jcrSessionPool = jcrSessionPool;
        if(jcrSessionPool.getRefreshInterval() != 0) {
            this.refresh_interval_minutes = jcrSessionPool.getRefreshInterval();
        }
    }
    
    public void increaseRefCount(){
        refCount++;
    } 
    public void decreaseRefCount(){
        refCount--;
    }
    public int getRefCount(){
        return this.refCount;
    }
    
    public boolean isValid(){
        if(dirty) {
            return false;
        }
        return ( (System.currentTimeMillis() - creationTime) < TIME_TO_LIVE_SECONDS*1000);
    }
    
    public Session getDelegatee(){
        return delegatee;
    }
    public JcrSessionPool getJcrSessionPool(){
        return this.jcrSessionPool;
    }
    
    public long getLastRefreshTick(){
        return this.lastRefresTick;
    }
    
    public int getLastRefreshedDivMinute(){
        return this.lastRefreshDivMinute;
    }
    
     public void logout() {
         log.warn("logout unsupported in read only session. Use the delegatee");
     }
    
     public void addLockToken(String lt) {
         log.warn("addLockToken unsupported in read only session");
     }
     
     public void refresh(boolean keepChanges) throws RepositoryException {
         if(lastRefresTick > JcrSessionPoolManager.getLastEventTick()) {
             // nothing changed in the repository, no need to refresh any session.
             return;
         }
         int crMin = Calendar.getInstance().get(Calendar.MINUTE);
         if(jcrSessionPool.isSessionInterValRefresh()) {
             if( (crMin/refresh_interval_minutes) != lastRefreshDivMinute) {
                 lastRefreshDivMinute = crMin/refresh_interval_minutes;
                 delegatee.refresh(keepChanges);
             }
         } else {
             delegatee.refresh(keepChanges);
         }
         
     }

     public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException, PathNotFoundException,
             VersionException, ConstraintViolationException, LockException, RepositoryException {
         log.warn("move is not allowed to in ReadOnlyPooledSession");
         throw new UnsupportedRepositoryOperationException("move is not allowed to in ReadOnlyPooledSession");
     }

     public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException,
             InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
         log.warn("save is not allowed to in ReadOnlyPooledSession");
         throw new UnsupportedRepositoryOperationException("save is not allowed to in ReadOnlyPooledSession");
     }

     public void removeLockToken(String lt) {
         log.warn("removeLockToken is not allowed to in ReadOnlyPooledSession");
     }

     public void setNamespacePrefix(String prefix, String uri) throws NamespaceException, RepositoryException {
         log.warn("setNamespacePrefix is not allowed to in ReadOnlyPooledSession");
         throw new UnsupportedRepositoryOperationException("setNamespacePrefix is not allowed to in ReadOnlyPooledSession");
     }
     
    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        delegatee.checkPermission(absPath, actions);
    }

    public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        delegatee.exportDocumentView(absPath, contentHandler, skipBinary, noRecurse);
    }

    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse)
            throws IOException, PathNotFoundException, RepositoryException {
        delegatee.exportDocumentView(absPath, out, skipBinary, noRecurse);
    }

    public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        delegatee.exportSystemView(absPath, contentHandler, skipBinary, noRecurse);
    }

    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse)
            throws IOException, PathNotFoundException, RepositoryException {
        delegatee.exportSystemView(absPath, out, skipBinary, noRecurse);
    }

    public Object getAttribute(String name) {
       return delegatee.getAttribute(name);
    }

    public String[] getAttributeNames() {
        return delegatee.getAttributeNames();
    }

    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException,
            ConstraintViolationException, VersionException, LockException, RepositoryException {    
        return delegatee.getImportContentHandler(parentAbsPath, uuidBehavior);
    }

    public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
        return delegatee.getItem(absPath);
    }

    public String[] getLockTokens() {
        return delegatee.getLockTokens();
    }

    public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException {
        return delegatee.getNamespacePrefix(uri);
    }

    public String[] getNamespacePrefixes() throws RepositoryException {
        return delegatee.getNamespacePrefixes();
    }

    public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException {
        return delegatee.getNamespaceURI(prefix);
    }

    public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
        return delegatee.getNodeByUUID(uuid);
    }

    public Repository getRepository() {
        return delegatee.getRepository();
    }

    public Node getRootNode() throws RepositoryException {
        return delegatee.getRootNode();
    }

    public String getUserID() {
        return delegatee.getUserID();
    }

    public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return delegatee.getValueFactory();
    }

    public Workspace getWorkspace() {
        return delegatee.getWorkspace();
    }

    public boolean hasPendingChanges() throws RepositoryException {
        return delegatee.hasPendingChanges();
    }

    public Session impersonate(Credentials credentials) throws LoginException, RepositoryException {
        return delegatee.impersonate(credentials);
    }

    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException,
            PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException,
            InvalidSerializedDataException, LockException, RepositoryException {
        
        delegatee.importXML(parentAbsPath, in, uuidBehavior);
    }

    public boolean isLive() {
        return delegatee.isLive();
    }

    public boolean itemExists(String absPath) throws RepositoryException {
        return delegatee.itemExists(absPath);
    }

    public void setIdleSince(long idleSince) {
        this.idleSince = idleSince;
    }

    public long getIdleSince(){
        return this.idleSince;
    }
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

}
