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
import com.github.gv2011.webdav.locking.LockedObject;
import com.github.gv2011.webdav.locking.ResourceLocks;
import com.github.gv2011.webdav.methods.DoCopy;
import com.github.gv2011.webdav.methods.DoDelete;
import com.github.gv2011.webdav.testutil.MockTest;

public class DoCopyTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static ByteArrayInputStream bais = new ByteArrayInputStream(resourceContent);
    static DelegatingServletInputStream dsis = new DelegatingServletInputStream(
            bais);

    @BeforeClass
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testDoCopyIfReadOnly() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        final ResourceLocks resLocks = new ResourceLocks();
        final DoDelete doDelete = new DoDelete(mockStore, resLocks, readOnly);

        final DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyOfLockNullResource() throws Exception {

        final String parentPath = "/lockedFolder";
        final String path = parentPath.concat("/nullFile");

        final String owner = new String("owner");
        final ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, parentPath, owner, true, 1,
                TEMP_TIMEOUT, !TEMPORARY);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("/destination"));

                oneOf(mockReq).getServerName();
                will(returnValue("myServer"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/destination"));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                final StoredObject so = initLockNullStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(so));

                oneOf(mockRes).addHeader("Allow",
                        "OPTIONS, MKCOL, PUT, PROPFIND, LOCK, UNLOCK");

                oneOf(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
            }
        });

        final DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        final DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    @Ignore("Broken, path /tmp/XMLTestFile must not be used.")
    public void testDoCopyIfParentIsLockedWithWrongLockToken() throws Exception {

        final String owner = new String("owner");
        final ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, destCollectionPath, owner, true, 1,
                TEMP_TIMEOUT, !TEMPORARY);

        final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                destCollectionPath);
        final String wrongLockToken = "(<opaquelocktoken:" + lo.getID()
                + "WRONG>)";

        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("myServer"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("If");
                will(returnValue(wrongLockToken));

                oneOf(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);

                oneOf(mockReq).getRequestURI();
                will(returnValue("http://foo.bar".concat(destCollectionPath)));

                oneOf(mockRes).getWriter();
                will(returnValue(pw));
            }
        });

        final DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        final DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfParentIsLockedWithRightLockToken() throws Exception {

        final String owner = new String("owner");
        final ResourceLocks resLocks = new ResourceLocks();

        resLocks.lock(mockTransaction, destCollectionPath, owner, true, 1,
                TEMP_TIMEOUT, !TEMPORARY);

        final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                destCollectionPath);
        final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("myServer"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("If");
                will(returnValue(rightLockToken));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                final StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction, destFilePath);

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destFilePath, dsis, null, null);
                will(returnValue(resourceLength));

                destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

            }
        });

        final DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        final DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfDestinationPathInvalid() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(null));

                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        final ResourceLocks resLocks = new ResourceLocks();
        final DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        final DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();

    }

    @Test
    public void testDoCopyIfSourceEqualsDestination() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);

            }
        });

        final ResourceLocks resLocks = new ResourceLocks();

        final DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        final DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyFolderIfNoLocks() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                final StoredObject sourceCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                final StoredObject destCollectionSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath);
                will(returnValue(destCollectionSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                oneOf(mockStore)
                        .createFolder(mockTransaction, destCollectionPath);

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("-1"));

                sourceChildren = new String[] { "sourceFile" };

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceChildren));

                final StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction,
                        destCollectionPath + "/sourceFile");

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destCollectionPath + "/sourceFile", dsis, null, null);

                final StoredObject destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath + "/sourceFile");
                will(returnValue(destFileSo));

            }
        });

        final ResourceLocks resLocks = new ResourceLocks();

        final DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        final DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfSourceDoesntExist() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                final StoredObject notExistSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(notExistSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);

            }
        });

        final ResourceLocks resLocks = new ResourceLocks();

        final DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        final DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfDestinationAlreadyExistsAndOverwriteTrue()
            throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                final StoredObject sourceSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/folder/destFolder"));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                final StoredObject existingDestSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(existingDestSo));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(existingDestSo));

                oneOf(mockStore).removeObject(mockTransaction, destFilePath);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockStore).createResource(mockTransaction, destFilePath);

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destFilePath, dsis, null, null);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(existingDestSo));

            }
        });

        final ResourceLocks resLocks = new ResourceLocks();
        final DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        final DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();

    }

    @Test
    public void testDoCopyIfDestinationAlreadyExistsAndOverwriteFalse()
            throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                final StoredObject sourceSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("serverName".concat(destFilePath)));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                final StoredObject existingDestSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(existingDestSo));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);

            }
        });

        final ResourceLocks resLocks = new ResourceLocks();
        final DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        final DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();

    }

    @Test
    public void testDoCopyIfOverwriteTrue() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                final StoredObject sourceSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("http://destination:80".concat(destFilePath)));

                oneOf(mockReq).getContextPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServletPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getAttribute("javax.servlet.include.path_info");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                final StoredObject destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockStore).removeObject(mockTransaction, destFilePath);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockStore).createResource(mockTransaction, destFilePath);

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destFilePath, dsis, null, null);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));
            }
        });

        final ResourceLocks resLocks = new ResourceLocks();
        final DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        final DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();

    }

    @Test
    public void testDoCopyIfOverwriteFalse() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                final StoredObject sourceSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("http://destination:80"
                        .concat(destCollectionPath)));

                oneOf(mockReq).getContextPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getAttribute("javax.servlet.include.path_info");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                final StoredObject existingDestSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath);
                will(returnValue(existingDestSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
            }
        });

        final ResourceLocks resLocks = new ResourceLocks();
        final DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        final DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();

    }
}
