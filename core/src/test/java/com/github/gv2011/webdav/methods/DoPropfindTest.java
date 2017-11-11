package com.github.gv2011.webdav.methods;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.gv2011.webdav.IMimeTyper;
import com.github.gv2011.webdav.ITransaction;
import com.github.gv2011.webdav.IWebdavStore;
import com.github.gv2011.webdav.StoredObject;
import com.github.gv2011.webdav.WebdavStatus;
import com.github.gv2011.webdav.locking.ResourceLocks;
import com.github.gv2011.webdav.methods.DoPropfind;
import com.github.gv2011.webdav.testutil.MockTest;

public class DoPropfindTest extends MockTest {
    static IWebdavStore mockStore;
    static IMimeTyper mockMimeTyper;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static PrintWriter printWriter;
    static byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l', 'l', 'o',
            '/', '>' };

    @BeforeClass
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockMimeTyper = _mockery.mock(IMimeTyper.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void doPropFindOnDirectory() throws Exception {
        final String path = "/";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("infinity"));

                final StoredObject rootSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(rootSo));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getContentLength();
                will(returnValue(0));
                // no content, which means it is a allprop request

                oneOf(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(printWriter));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, path);
                will(returnValue("text/xml; charset=UTF-8"));

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(rootSo));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue(path));

                oneOf(mockStore).getChildrenNames(mockTransaction, path);
                will(returnValue(new String[] { "file1", "file2" }));

                final StoredObject file1So = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path + "file1");
                will(returnValue(file1So));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue(path));

                oneOf(mockStore)
                        .getChildrenNames(mockTransaction, path + "file1");
                will(returnValue(new String[] {}));

                final StoredObject file2So = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path + "file2");
                will(returnValue(file2So));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue(path));

                oneOf(mockStore)
                        .getChildrenNames(mockTransaction, path + "file2");
                will(returnValue(new String[] {}));
            }
        });

        final DoPropfind doPropfind = new DoPropfind(mockStore, new ResourceLocks(),
                mockMimeTyper);
        doPropfind.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void doPropFindOnFile() throws Exception {
        final String path = "/testFile";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("0"));

                final StoredObject fileSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getContentLength();
                will(returnValue(0));
                // no content, which means it is a allprop request

                oneOf(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(printWriter));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, path);
                will(returnValue("text/xml; charset=UTF-8"));

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue("/"));
            }
        });

        final DoPropfind doPropfind = new DoPropfind(mockStore, new ResourceLocks(),
                mockMimeTyper);

        doPropfind.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void doPropFindOnNonExistingResource() throws Exception {
        final String path = "/notExists";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("0"));

                final StoredObject notExistingSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(notExistingSo));

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockReq).getRequestURI();
                will(returnValue(path));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND, path);
            }
        });

        final DoPropfind doPropfind = new DoPropfind(mockStore, new ResourceLocks(),
                mockMimeTyper);

        doPropfind.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

}
