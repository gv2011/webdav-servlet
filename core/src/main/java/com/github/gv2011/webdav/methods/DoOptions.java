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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.gv2011.webdav.ITransaction;
import com.github.gv2011.webdav.IWebdavStore;
import com.github.gv2011.webdav.StoredObject;
import com.github.gv2011.webdav.WebdavStatus;
import com.github.gv2011.webdav.exceptions.AccessDeniedException;
import com.github.gv2011.webdav.exceptions.LockFailedException;
import com.github.gv2011.webdav.exceptions.WebdavException;
import com.github.gv2011.webdav.locking.ResourceLocks;

public class DoOptions extends DeterminableMethod {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoOptions.class);

    private final IWebdavStore _store;
    private final ResourceLocks _resourceLocks;

    public DoOptions(final IWebdavStore store, final ResourceLocks resLocks) {
        _store = store;
        _resourceLocks = resLocks;
    }

    @Override
    public void execute(final ITransaction transaction, final HttpServletRequest req,
            final HttpServletResponse resp) throws IOException, LockFailedException {

        LOG.trace("-- " + this.getClass().getName());

        final String tempLockOwner = "doOptions" + System.currentTimeMillis()
                + req.toString();
        final String path = getRelativePath(req);
        if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0,
                TEMP_TIMEOUT, TEMPORARY)) {
            StoredObject so = null;
            try {
                resp.addHeader("DAV", "1, 2");

                so = _store.getStoredObject(transaction, path);
                final String methodsAllowed = determineMethodsAllowed(so);
                resp.addHeader("Allow", methodsAllowed);
                resp.addHeader("MS-Author-Via", "DAV");
            } catch (final AccessDeniedException e) {
                resp.sendError(WebdavStatus.SC_FORBIDDEN);
            } catch (final WebdavException e) {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            } finally {
                _resourceLocks.unlockTemporaryLockedObjects(transaction, path,
                        tempLockOwner);
            }
        } else {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
