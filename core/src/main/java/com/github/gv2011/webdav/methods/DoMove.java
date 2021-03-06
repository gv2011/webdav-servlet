/*
 * Copyright 1999,2004 The Apache Software Foundation.
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
package com.github.gv2011.webdav.methods;

import java.io.IOException;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.gv2011.webdav.ITransaction;
import com.github.gv2011.webdav.WebdavStatus;
import com.github.gv2011.webdav.exceptions.AccessDeniedException;
import com.github.gv2011.webdav.exceptions.LockFailedException;
import com.github.gv2011.webdav.exceptions.ObjectAlreadyExistsException;
import com.github.gv2011.webdav.exceptions.WebdavException;
import com.github.gv2011.webdav.locking.ResourceLocks;

public class DoMove extends AbstractMethod {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoMove.class);

    private final ResourceLocks _resourceLocks;
    private final DoDelete _doDelete;
    private final DoCopy _doCopy;
    private final boolean _readOnly;

    public DoMove(final ResourceLocks resourceLocks, final DoDelete doDelete,
            final DoCopy doCopy, final boolean readOnly) {
        _resourceLocks = resourceLocks;
        _doDelete = doDelete;
        _doCopy = doCopy;
        _readOnly = readOnly;
    }

    @Override
    public void execute(final ITransaction transaction, final HttpServletRequest req,
            final HttpServletResponse resp) throws IOException, LockFailedException {

        if (!_readOnly) {
            LOG.trace("-- " + this.getClass().getName());

            final String sourcePath = getRelativePath(req);
            Hashtable<String, Integer> errorList = new Hashtable<>();

            if (!checkLocks(transaction, req, resp, _resourceLocks, sourcePath)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return;
            }

            final String destinationPath = req.getHeader("Destination");
            if (destinationPath == null) {
                resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                return;
            }

            if (!checkLocks(transaction, req, resp, _resourceLocks,
                    destinationPath)) {
                resp.setStatus(WebdavStatus.SC_LOCKED);
                return;
            }

            final String tempLockOwner = "doMove" + System.currentTimeMillis()
                    + req.toString();

            if (_resourceLocks.lock(transaction, sourcePath, tempLockOwner,
                    false, 0, TEMP_TIMEOUT, TEMPORARY)) {
                try {

                    if (_doCopy.copyResource(transaction, req, resp)) {

                        errorList = new Hashtable<>();
                        _doDelete.deleteResource(transaction, sourcePath,
                                errorList, req, resp);
                        if (!errorList.isEmpty()) {
                            sendReport(req, resp, errorList);
                        }
                    }

                } catch (final AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (final ObjectAlreadyExistsException e) {
                    resp.sendError(WebdavStatus.SC_NOT_FOUND, req
                            .getRequestURI());
                } catch (final WebdavException e) {
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                } finally {
                    _resourceLocks.unlockTemporaryLockedObjects(transaction,
                            sourcePath, tempLockOwner);
                }
            } else {
                errorList.put(req.getHeader("Destination"),
                        WebdavStatus.SC_LOCKED);
                sendReport(req, resp, errorList);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);

        }

    }

}
