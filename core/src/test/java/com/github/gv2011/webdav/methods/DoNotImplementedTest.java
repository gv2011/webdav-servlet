package com.github.gv2011.webdav.methods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.gv2011.webdav.ITransaction;
import com.github.gv2011.webdav.IWebdavStore;
import com.github.gv2011.webdav.WebdavStatus;
import com.github.gv2011.webdav.methods.DoNotImplemented;
import com.github.gv2011.webdav.testutil.MockTest;

public class DoNotImplementedTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;

    @BeforeClass
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testDoNotImplementedIfReadOnlyTrue() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("notImplementedMethod"));
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        final DoNotImplemented doNotImplemented = new DoNotImplemented(readOnly);
        doNotImplemented.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoNotImplementedIfReadOnlyFalse() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("notImplementedMethod"));
                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_IMPLEMENTED);
            }
        });

        final DoNotImplemented doNotImplemented = new DoNotImplemented(!readOnly);
        doNotImplemented.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }
}
