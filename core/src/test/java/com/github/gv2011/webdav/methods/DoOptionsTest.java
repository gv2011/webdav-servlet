package com.github.gv2011.webdav.methods;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.gv2011.webdav.IMimeTyper;
import com.github.gv2011.webdav.ITransaction;
import com.github.gv2011.webdav.IWebdavStore;
import com.github.gv2011.webdav.StoredObject;
import com.github.gv2011.webdav.exceptions.LockFailedException;
import com.github.gv2011.webdav.locking.ResourceLocks;
import com.github.gv2011.webdav.methods.DoOptions;
import com.github.gv2011.webdav.testutil.MockTest;

public class DoOptionsTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static IMimeTyper mockMimeTyper;
    static ITransaction mockTransaction;
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
    public void testOptionsOnExistingNode() throws IOException,
            LockFailedException {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                oneOf(mockRes).addHeader("DAV", "1, 2");

                final StoredObject indexSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockRes).addHeader(
                        "Allow",
                        "OPTIONS, GET, HEAD, POST, DELETE, "
                                + "TRACE, PROPPATCH, COPY, "
                                + "MOVE, LOCK, UNLOCK, PROPFIND");

                oneOf(mockRes).addHeader("MS-Author-Via", "DAV");
            }
        });

        final DoOptions doOptions = new DoOptions(mockStore, new ResourceLocks());
        doOptions.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testOptionsOnNonExistingNode() throws IOException,
            LockFailedException {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                oneOf(mockRes).addHeader("DAV", "1, 2");

                final StoredObject indexSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockRes).addHeader("Allow", "OPTIONS, MKCOL, PUT");

                oneOf(mockRes).addHeader("MS-Author-Via", "DAV");
            }
        });

        final DoOptions doOptions = new DoOptions(mockStore, new ResourceLocks());
        doOptions.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

}
