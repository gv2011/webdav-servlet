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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.gv2011.webdav.methods;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.gv2011.webdav.IMimeTyper;
import com.github.gv2011.webdav.ITransaction;
import com.github.gv2011.webdav.IWebdavStore;
import com.github.gv2011.webdav.StoredObject;
import com.github.gv2011.webdav.WebdavStatus;
import com.github.gv2011.webdav.exceptions.AccessDeniedException;
import com.github.gv2011.webdav.exceptions.LockFailedException;
import com.github.gv2011.webdav.exceptions.ObjectAlreadyExistsException;
import com.github.gv2011.webdav.exceptions.WebdavException;
import com.github.gv2011.webdav.locking.ResourceLocks;

public class DoHead extends AbstractMethod {

    protected String _dftIndexFile;
    protected IWebdavStore _store;
    protected String _insteadOf404;
    protected ResourceLocks _resourceLocks;
    protected IMimeTyper _mimeTyper;
    protected int _contentLength;

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoHead.class);

    public DoHead(final IWebdavStore store, final String dftIndexFile, final String insteadOf404,
            final ResourceLocks resourceLocks, final IMimeTyper mimeTyper,
            final int contentLengthHeader) {
        _store = store;
        _dftIndexFile = dftIndexFile;
        _insteadOf404 = insteadOf404;
        _resourceLocks = resourceLocks;
        _mimeTyper = mimeTyper;
        _contentLength = contentLengthHeader;
    }

    @Override
    public void execute(final ITransaction transaction, final HttpServletRequest req,
            final HttpServletResponse resp) throws IOException, LockFailedException {

        // determines if the uri exists.

        boolean bUriExists = false;

        String path = getRelativePath(req);
        LOG.trace("-- " + this.getClass().getName());

        StoredObject so;
        try {
            so = _store.getStoredObject(transaction, path);
            if (so == null) {
                if (_insteadOf404 != null && !_insteadOf404.trim().equals("")) {
                    path = _insteadOf404;
                    so = _store.getStoredObject(transaction, _insteadOf404);
                }
            } else
                bUriExists = true;
        } catch (final AccessDeniedException e) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
            return;
        }

        if (so != null) {
            if (so.isFolder()) {
                if (_dftIndexFile != null && !_dftIndexFile.trim().equals("")) {
                    resp.sendRedirect(resp.encodeRedirectURL(req
                            .getRequestURI()
                            + _dftIndexFile));
                    return;
                }
            } else if (so.isNullResource()) {
                final String methodsAllowed = DeterminableMethod
                        .determineMethodsAllowed(so);
                resp.addHeader("Allow", methodsAllowed);
                resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                return;
            }

            final String tempLockOwner = "doGet" + System.currentTimeMillis()
                    + req.toString();

            if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0,
                    TEMP_TIMEOUT, TEMPORARY)) {
                try {

                    final String eTagMatch = req.getHeader("If-None-Match");
                    if (eTagMatch != null) {
                        if (eTagMatch.equals(getETag(so))) {
                            resp.setStatus(WebdavStatus.SC_NOT_MODIFIED);
                            return;
                        }
                    }

                    if (so.isResource()) {
                        // path points to a file but ends with / or \
                        if (path.endsWith("/") || (path.endsWith("\\"))) {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                                    req.getRequestURI());
                        } else {

                            // setting headers
                            final long lastModified = so.getLastModified().getTime();
                            resp.setDateHeader("last-modified", lastModified);

                            final String eTag = getETag(so);
                            resp.addHeader("ETag", eTag);

                            final long resourceLength = so.getResourceLength();

                            if (_contentLength == 1) {
                                if (resourceLength > 0) {
                                    if (resourceLength <= Integer.MAX_VALUE) {
                                        resp
                                                .setContentLength((int) resourceLength);
                                    } else {
                                        resp.setHeader("content-length", ""
                                                + resourceLength);
                                        // is "content-length" the right header?
                                        // is long a valid format?
                                    }
                                }
                            }

                            final String mimeType = _mimeTyper.getMimeType(transaction, path);
                            if (mimeType != null) {
                                resp.setContentType(mimeType);
                            } else {
                                final int lastSlash = path.replace('\\', '/')
                                        .lastIndexOf('/');
                                final int lastDot = path.indexOf(".", lastSlash);
                                if (lastDot == -1) {
                                    resp.setContentType("text/html");
                                }
                            }

                            doBody(transaction, resp, path);
                        }
                    } else {
                        folderBody(transaction, path, resp, req);
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
                            path, tempLockOwner);
                }
            } else {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            folderBody(transaction, path, resp, req);
        }

        if (!bUriExists)
            resp.setStatus(WebdavStatus.SC_NOT_FOUND);

    }

    protected void folderBody(final ITransaction transaction, final String path,
            final HttpServletResponse resp, final HttpServletRequest req)
            throws IOException {
        // no body for HEAD
    }

    protected void doBody(final ITransaction transaction, final HttpServletResponse resp,
            final String path) throws IOException {
        // no body for HEAD
    }
}
