package com.github.gv2011.webdav.methods;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.gv2011.webdav.IMethodExecutor;
import com.github.gv2011.webdav.ITransaction;
import com.github.gv2011.webdav.WebdavStatus;

public class DoNotImplemented implements IMethodExecutor {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoNotImplemented.class);
    private final boolean _readOnly;

    public DoNotImplemented(final boolean readOnly) {
        _readOnly = readOnly;
    }

    @Override
    public void execute(final ITransaction transaction, final HttpServletRequest req,
            final HttpServletResponse resp) throws IOException {
        LOG.trace("-- " + req.getMethod());

        if (_readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } else
            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }
}
