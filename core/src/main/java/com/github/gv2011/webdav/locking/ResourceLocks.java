/*
 * Copyright 2005-2006 webdav-servlet group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.gv2011.webdav.locking;

import java.util.Enumeration;
import java.util.Hashtable;

import com.github.gv2011.webdav.ITransaction;
import com.github.gv2011.webdav.exceptions.LockFailedException;

/**
 * simple locking management for concurrent data access, NOT the webdav locking.
 * ( could that be used instead? )
 *
 * IT IS ACTUALLY USED FOR DOLOCK
 *
 * @author re
 */
public class ResourceLocks implements IResourceLocks {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(ResourceLocks.class);

    /**
     * after creating this much LockedObjects, a cleanup deletes unused
     * LockedObjects
     */
    private final int _cleanupLimit = 100000;

    protected int _cleanupCounter = 0;

    /**
     * keys: path value: LockedObject from that path
     */
    protected Hashtable<String, LockedObject> _locks = new Hashtable<>();

    /**
     * keys: id value: LockedObject from that id
     */
    protected Hashtable<String, LockedObject> _locksByID = new Hashtable<>();

    /**
     * keys: path value: Temporary LockedObject from that path
     */
    protected Hashtable<String, LockedObject> _tempLocks = new Hashtable<>();

    /**
     * keys: id value: Temporary LockedObject from that id
     */
    protected Hashtable<String, LockedObject> _tempLocksByID = new Hashtable<>();

    // REMEMBER TO REMOVE UNUSED LOCKS FROM THE HASHTABLE AS WELL

    protected LockedObject _root = null;

    protected LockedObject _tempRoot = null;

    private final boolean _temporary = true;

    public ResourceLocks() {
        _root = new LockedObject(this, "/", true);
        _tempRoot = new LockedObject(this, "/", false);
    }

    @Override
    public synchronized boolean lock(final ITransaction transaction, final String path,
            final String owner, final boolean exclusive, final int depth, final int timeout,
            final boolean temporary) throws LockFailedException {

        LockedObject lo = null;

        if (temporary) {
            lo = generateTempLockedObjects(transaction, path);
            lo._type = "read";
        } else {
            lo = generateLockedObjects(transaction, path);
            lo._type = "write";
        }

        if (lo.checkLocks(exclusive, depth)) {

            lo._exclusive = exclusive;
            lo._lockDepth = depth;
            lo._expiresAt = System.currentTimeMillis() + (timeout * 1000);
            if (lo._parent != null) {
                lo._parent._expiresAt = lo._expiresAt;
                if (lo._parent.equals(_root)) {
                    final LockedObject rootLo = getLockedObjectByPath(transaction,
                            _root.getPath());
                    rootLo._expiresAt = lo._expiresAt;
                } else if (lo._parent.equals(_tempRoot)) {
                    final LockedObject tempRootLo = getTempLockedObjectByPath(
                            transaction, _tempRoot.getPath());
                    tempRootLo._expiresAt = lo._expiresAt;
                }
            }
            if (lo.addLockedObjectOwner(owner)) {
                return true;
            } else {
                LOG.trace("Couldn't set owner \"" + owner
                        + "\" to resource at '" + path + "'");
                return false;
            }
        } else {
            // can not lock
            LOG.trace("Lock resource at " + path + " failed because"
                    + "\na parent or child resource is currently locked");
            return false;
        }
    }

    @Override
    public synchronized boolean unlock(final ITransaction transaction, final String id,
            final String owner) {

        if (_locksByID.containsKey(id)) {
            final String path = _locksByID.get(id).getPath();
            if (_locks.containsKey(path)) {
                final LockedObject lo = _locks.get(path);
                lo.removeLockedObjectOwner(owner);

                if (lo._children == null && lo._owner == null)
                    lo.removeLockedObject();

            } else {
                // there is no lock at that path. someone tried to unlock it
                // anyway. could point to a problem
                LOG
                        .trace("net.sf.webdav.locking.ResourceLocks.unlock(): no lock for path "
                                + path);
                return false;
            }

            if (_cleanupCounter > _cleanupLimit) {
                _cleanupCounter = 0;
                cleanLockedObjects(transaction, _root, !_temporary);
            }
        }
        checkTimeouts(transaction, !_temporary);

        return true;

    }

    @Override
    public synchronized void unlockTemporaryLockedObjects(
            final ITransaction transaction, final String path, final String owner) {
        if (_tempLocks.containsKey(path)) {
            final LockedObject lo = _tempLocks.get(path);
            lo.removeLockedObjectOwner(owner);

        } else {
            // there is no lock at that path. someone tried to unlock it
            // anyway. could point to a problem
            LOG
                    .trace("net.sf.webdav.locking.ResourceLocks.unlock(): no lock for path "
                            + path);
        }

        if (_cleanupCounter > _cleanupLimit) {
            _cleanupCounter = 0;
            cleanLockedObjects(transaction, _tempRoot, _temporary);
        }

        checkTimeouts(transaction, _temporary);

    }

    @Override
    public void checkTimeouts(final ITransaction transaction, final boolean temporary) {
        if (!temporary) {
            final Enumeration<LockedObject> lockedObjects = _locks.elements();
            while (lockedObjects.hasMoreElements()) {
                final LockedObject currentLockedObject = lockedObjects.nextElement();

                if (currentLockedObject._expiresAt < System.currentTimeMillis()) {
                    currentLockedObject.removeLockedObject();
                }
            }
        } else {
            final Enumeration<LockedObject> lockedObjects = _tempLocks.elements();
            while (lockedObjects.hasMoreElements()) {
                final LockedObject currentLockedObject = lockedObjects.nextElement();

                if (currentLockedObject._expiresAt < System.currentTimeMillis()) {
                    currentLockedObject.removeTempLockedObject();
                }
            }
        }

    }

    @Override
    public boolean exclusiveLock(final ITransaction transaction, final String path,
            final String owner, final int depth, final int timeout) throws LockFailedException {
        return lock(transaction, path, owner, true, depth, timeout, false);
    }

    @Override
    public boolean sharedLock(final ITransaction transaction, final String path,
            final String owner, final int depth, final int timeout) throws LockFailedException {
        return lock(transaction, path, owner, false, depth, timeout, false);
    }

    @Override
    public LockedObject getLockedObjectByID(final ITransaction transaction, final String id) {
        if (_locksByID.containsKey(id)) {
            return _locksByID.get(id);
        } else {
            return null;
        }
    }

    @Override
    public LockedObject getLockedObjectByPath(final ITransaction transaction,
            final String path) {
        if (_locks.containsKey(path)) {
            return _locks.get(path);
        } else {
            return null;
        }
    }

    @Override
    public LockedObject getTempLockedObjectByID(final ITransaction transaction,
            final String id) {
        if (_tempLocksByID.containsKey(id)) {
            return _tempLocksByID.get(id);
        } else {
            return null;
        }
    }

    @Override
    public LockedObject getTempLockedObjectByPath(final ITransaction transaction,
            final String path) {
        if (_tempLocks.containsKey(path)) {
            return _tempLocks.get(path);
        } else {
            return null;
        }
    }

    /**
     * generates real LockedObjects for the resource at path and its parent
     * folders. does not create new LockedObjects if they already exist
     *
     * @param transaction
     * @param path
     *      path to the (new) LockedObject
     * @return the LockedObject for path.
     */
    private LockedObject generateLockedObjects(final ITransaction transaction,
            final String path) {
        if (!_locks.containsKey(path)) {
            final LockedObject returnObject = new LockedObject(this, path,
                    !_temporary);
            final String parentPath = getParentPath(path);
            if (parentPath != null) {
                final LockedObject parentLockedObject = generateLockedObjects(
                        transaction, parentPath);
                parentLockedObject.addChild(returnObject);
                returnObject._parent = parentLockedObject;
            }
            return returnObject;
        } else {
            // there is already a LockedObject on the specified path
            return _locks.get(path);
        }

    }

    /**
     * generates temporary LockedObjects for the resource at path and its parent
     * folders. does not create new LockedObjects if they already exist
     *
     * @param transaction
     * @param path
     *      path to the (new) LockedObject
     * @return the LockedObject for path.
     */
    private LockedObject generateTempLockedObjects(final ITransaction transaction,
            final String path) {
        if (!_tempLocks.containsKey(path)) {
            final LockedObject returnObject = new LockedObject(this, path, _temporary);
            final String parentPath = getParentPath(path);
            if (parentPath != null) {
                final LockedObject parentLockedObject = generateTempLockedObjects(
                        transaction, parentPath);
                parentLockedObject.addChild(returnObject);
                returnObject._parent = parentLockedObject;
            }
            return returnObject;
        } else {
            // there is already a LockedObject on the specified path
            return _tempLocks.get(path);
        }

    }

    /**
     * deletes unused LockedObjects and resets the counter. works recursively
     * starting at the given LockedObject
     *
     * @param transaction
     * @param lo
     *      LockedObject
     * @param temporary
     *      Clean temporary or real locks
     *
     * @return if cleaned
     */
    private boolean cleanLockedObjects(final ITransaction transaction,
            final LockedObject lo, final boolean temporary) {

        if (lo._children == null) {
            if (lo._owner == null) {
                if (temporary) {
                    lo.removeTempLockedObject();
                } else {
                    lo.removeLockedObject();
                }

                return true;
            } else {
                return false;
            }
        } else {
            boolean canDelete = true;
            int limit = lo._children.length;
            for (int i = 0; i < limit; i++) {
                if (!cleanLockedObjects(transaction, lo._children[i], temporary)) {
                    canDelete = false;
                } else {

                    // because the deleting shifts the array
                    i--;
                    limit--;
                }
            }
            if (canDelete) {
                if (lo._owner == null) {
                    if (temporary) {
                        lo.removeTempLockedObject();
                    } else {
                        lo.removeLockedObject();
                    }
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    /**
     * creates the parent path from the given path by removing the last '/' and
     * everything after that
     *
     * @param path
     *      the path
     * @return parent path
     */
    private String getParentPath(final String path) {
        final int slash = path.lastIndexOf('/');
        if (slash == -1) {
            return null;
        } else {
            if (slash == 0) {
                // return "root" if parent path is empty string
                return "/";
            } else {
                return path.substring(0, slash);
            }
        }
    }

}
