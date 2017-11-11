package com.github.gv2011.webdav.methods;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

import com.github.gv2011.webdav.ITransaction;
import com.github.gv2011.webdav.IWebdavStore;
import com.github.gv2011.webdav.StoredObject;
import com.github.gv2011.webdav.WebdavStatus;
import com.github.gv2011.webdav.locking.IResourceLocks;
import com.github.gv2011.webdav.locking.LockedObject;
import com.github.gv2011.webdav.locking.ResourceLocks;
import com.github.gv2011.webdav.methods.DoLock;
import com.github.gv2011.webdav.methods.DoMkcol;
import com.github.gv2011.webdav.testutil.MockTest;

public class DoMkcolTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static IResourceLocks mockResourceLocks;

    static String parentPath = "/parentCollection";
    static String mkcolPath = parentPath.concat("/makeCollection");
    static String owner = "a lock owner";

    @BeforeClass
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
        mockResourceLocks = _mockery.mock(IResourceLocks.class);
    }

    @Test
    public void testMkcolIfReadOnlyIsTrue() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        final ResourceLocks resLocks = new ResourceLocks();
        final DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolSuccess() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                final StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                final StoredObject mkcolSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(mkcolSo));

                oneOf(mockStore).createFolder(mockTransaction, mkcolPath);

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

            }
        });

        final ResourceLocks resLocks = new ResourceLocks();
        final DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolIfParentPathIsNoFolder() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                final StoredObject parentSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                final String methodsAllowed = "OPTIONS, GET, HEAD, POST, DELETE, TRACE, "
                        + "PROPPATCH, COPY, MOVE, LOCK, UNLOCK, PROPFIND";

                oneOf(mockRes).addHeader("Allow", methodsAllowed);

                oneOf(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
            }
        });

        final ResourceLocks resLocks = new ResourceLocks();
        final DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolIfParentPathIsAFolderButObjectAlreadyExists()
            throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                final StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                final StoredObject mkcolSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(mkcolSo));

                oneOf(mockRes)
                        .addHeader(
                                "Allow",
                                "OPTIONS, GET, HEAD, POST, DELETE, TRACE, PROPPATCH, COPY, MOVE, LOCK, UNLOCK, PROPFIND, PUT");

                oneOf(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);

            }
        });

        final ResourceLocks resLocks = new ResourceLocks();
        final DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolIfParentFolderIsLockedWithRightLockToken()
            throws Exception {

        final ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, parentPath, owner, true, -1, 200, false);
        final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                parentPath);
        final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                oneOf(mockReq).getHeader("If");
                will(returnValue(rightLockToken));

                final StoredObject parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                final StoredObject mkcolSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(mkcolSo));

                oneOf(mockStore).createFolder(mockTransaction, mkcolPath);

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

            }
        });

        final DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testMkcolIfParentFolderIsLockedWithWrongLockToken()
            throws Exception {

        final ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, parentPath, owner, true, -1, 200, false);
        final String wrongLockToken = "(<opaquelocktoken:" + "aWrongLockToken"
                + ">)";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                oneOf(mockReq).getHeader("If");
                will(returnValue(wrongLockToken));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        final DoMkcol doMkcol = new DoMkcol(mockStore, resLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    @Ignore("Broken, path /tmp/XMLTestFile must not be used.")
    public void testMkcolOnALockNullResource() throws Exception {

        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        final ByteArrayInputStream baisExclusive = new ByteArrayInputStream(
                exclusiveLockRequestByteArray);
        final DelegatingServletInputStream dsisExclusive = new DelegatingServletInputStream(
                baisExclusive);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                LockedObject lockNullResourceLo = null;

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        mkcolPath);
                will(returnValue(lockNullResourceLo));

                final LockedObject parentLo = null;

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        parentPath);
                will(returnValue(parentLo));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                oneOf(mockResourceLocks).lock(with(any(ITransaction.class)),
                        with(any(String.class)), with(any(String.class)),
                        with(any(boolean.class)), with(any(int.class)),
                        with(any(int.class)), with(any(boolean.class)));
                will(returnValue(true));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject lockNullResourceSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(lockNullResourceSo));

                StoredObject parentSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockStore).createFolder(mockTransaction, parentPath);

                parentSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(lockNullResourceSo));

                oneOf(mockStore).createResource(mockTransaction, mkcolPath);

                lockNullResourceSo = initLockNullStoredObject();

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(lockNullResourceSo));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsisExclusive));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(("0")));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                final ResourceLocks resLocks = ResourceLocks.class.newInstance();

                oneOf(mockResourceLocks).exclusiveLock(mockTransaction,
                        mkcolPath, "I'am the Lock Owner", 0, 604800);
                will(returnValue(true));

                lockNullResourceLo = initLockNullLockedObject(resLocks,
                        mkcolPath);

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        mkcolPath);
                will(returnValue(lockNullResourceLo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(pw));

                String loId = null;
                if (lockNullResourceLo != null) {
                    loId = lockNullResourceLo.getID();
                }
                final String lockToken = "<opaquelocktoken:" + loId + ">";

                oneOf(mockRes).addHeader("Lock-Token", lockToken);

                oneOf(mockResourceLocks).unlockTemporaryLockedObjects(
                        with(any(ITransaction.class)), with(any(String.class)),
                        with(any(String.class)));

                // -----LOCK on a non-existing resource successful------
                // --------now MKCOL on the lock-null resource----------

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(mkcolPath));

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        parentPath);
                will(returnValue(parentLo));

                oneOf(mockResourceLocks).lock(with(any(ITransaction.class)),
                        with(any(String.class)), with(any(String.class)),
                        with(any(boolean.class)), with(any(int.class)),
                        with(any(int.class)), with(any(boolean.class)));
                will(returnValue(true));

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockStore).getStoredObject(mockTransaction, mkcolPath);
                will(returnValue(lockNullResourceSo));

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        mkcolPath);
                will(returnValue(lockNullResourceLo));

                final String ifHeaderLockToken = "(<locktoken:" + loId + ">)";

                oneOf(mockReq).getHeader("If");
                will(returnValue(ifHeaderLockToken));

                final String[] owners = lockNullResourceLo.getOwner();
                String owner = null;
                if (owners != null)
                    owner = owners[0];

                oneOf(mockResourceLocks).unlock(mockTransaction, loId, owner);
                will(returnValue(true));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockResourceLocks).unlockTemporaryLockedObjects(
                        with(any(ITransaction.class)), with(any(String.class)),
                        with(any(String.class)));

            }
        });

        final DoLock doLock = new DoLock(mockStore, mockResourceLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        final DoMkcol doMkcol = new DoMkcol(mockStore, mockResourceLocks, !readOnly);
        doMkcol.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }
}
