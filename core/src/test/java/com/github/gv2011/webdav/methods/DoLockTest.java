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
import com.github.gv2011.webdav.testutil.MockTest;

public class DoLockTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static IResourceLocks mockResourceLocks;

    static boolean exclusive = true;
    static String depthString = "-1";
    static int depth = -1;
    static String timeoutString = "10";

    @BeforeClass
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
        mockResourceLocks = _mockery.mock(IResourceLocks.class);
    }

    @Test
    public void testDoLockIfReadOnly() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        final ResourceLocks resLocks = new ResourceLocks();

        final DoLock doLock = new DoLock(mockStore, resLocks, readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    @Ignore("Broken, path /tmp/XMLTestFile must not be used.")
    public void testDoRefreshLockOnLockedResource() throws Exception {

        final String lockPath = "/aFileToLock";
        final String lockOwner = "owner";

        final ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, lockOwner, exclusive, depth,
                TEMP_TIMEOUT, !TEMPORARY);

        final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                lockPath);
        final String lockTokenString = lo.getID();
        final String lockToken = "(<opaquelocktoken:" + lockTokenString + ">)";

        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("If");
                will(returnValue(lockToken));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                exactly(2).of(mockReq).getHeader("If");
                will(returnValue(lockToken));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(pw));

                oneOf(mockRes).addHeader(
                        "Lock-Token",
                        lockToken.substring(lockToken.indexOf("(") + 1,
                                lockToken.indexOf(")")));
            }
        });

        final DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    @Ignore("Broken, path /tmp/XMLTestFile must not be used.")
   public void testDoExclusiveLockOnResource() throws Exception {

        final String lockPath = "/aFileToLock";

        final ResourceLocks resLocks = new ResourceLocks();
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
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                final StoredObject so = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsisExclusive));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(depthString));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue(timeoutString));

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(pw));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                oneOf(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));
            }
        });

        final DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    @Ignore("Broken, path /tmp/XMLTestFile must not be used.")
    public void testDoSharedLockOnResource() throws Exception {

        final String lockPath = "/aFileToLock";

        final ResourceLocks resLocks = new ResourceLocks();
        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        final ByteArrayInputStream baisShared = new ByteArrayInputStream(
                sharedLockRequestByteArray);
        final DelegatingServletInputStream dsisShared = new DelegatingServletInputStream(
                baisShared);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                final StoredObject so = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsisShared));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(depthString));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue(timeoutString));

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(pw));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                oneOf(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));
            }
        });

        final DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    @Ignore("Broken, path /tmp/XMLTestFile must not be used.")
    public void testDoExclusiveLockOnCollection() throws Exception {

        final String lockPath = "/aFolderToLock";

        final ResourceLocks resLocks = new ResourceLocks();

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
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                final StoredObject so = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsisExclusive));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(depthString));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue(timeoutString));

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(pw));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                oneOf(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));
            }
        });

        final DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    @Ignore("Broken, path /tmp/XMLTestFile must not be used.")
    public void testDoSharedLockOnCollection() throws Exception {

        final String lockPath = "/aFolderToLock";

        final ResourceLocks resLocks = new ResourceLocks();
        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        final ByteArrayInputStream baisShared = new ByteArrayInputStream(
                sharedLockRequestByteArray);
        final DelegatingServletInputStream dsisShared = new DelegatingServletInputStream(
                baisShared);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                oneOf(mockReq).getHeader("If");
                will(returnValue(null));

                final StoredObject so = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsisShared));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(depthString));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue(timeoutString));

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(pw));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                oneOf(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));
            }
        });

        final DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    @Ignore("Broken, path /tmp/XMLTestFile must not be used.")
    public void testDoLockNullResourceLock() throws Exception {

        final String parentPath = "/parentCollection";
        final String lockPath = parentPath.concat("/aNullResource");

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
                will(returnValue(lockPath));

                LockedObject lockNullResourceLo = null;

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        lockPath);
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

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockNullResourceSo));

                final StoredObject parentSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                oneOf(mockStore).createFolder(mockTransaction, parentPath);

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockNullResourceSo));

                oneOf(mockStore).createResource(mockTransaction, lockPath);

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                lockNullResourceSo = initLockNullStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockNullResourceSo));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsisExclusive));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(("0")));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                final ResourceLocks resLocks = ResourceLocks.class.newInstance();

                oneOf(mockResourceLocks).exclusiveLock(mockTransaction, lockPath,
                        "I'am the Lock Owner", 0, 604800);
                will(returnValue(true));

                lockNullResourceLo = initLockNullLockedObject(resLocks,
                        lockPath);

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        lockPath);
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
            }
        });

        final DoLock doLock = new DoLock(mockStore, mockResourceLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();

    }
}
