package com.github.gv2011.webdav;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.gv2011.webdav.exceptions.UnauthenticatedException;
import com.github.gv2011.webdav.exceptions.WebdavException;
import com.github.gv2011.webdav.fromcatalina.MD5Encoder;
import com.github.gv2011.webdav.locking.ResourceLocks;
import com.github.gv2011.webdav.methods.DoCopy;
import com.github.gv2011.webdav.methods.DoDelete;
import com.github.gv2011.webdav.methods.DoGet;
import com.github.gv2011.webdav.methods.DoHead;
import com.github.gv2011.webdav.methods.DoLock;
import com.github.gv2011.webdav.methods.DoMkcol;
import com.github.gv2011.webdav.methods.DoMove;
import com.github.gv2011.webdav.methods.DoNotImplemented;
import com.github.gv2011.webdav.methods.DoOptions;
import com.github.gv2011.webdav.methods.DoPropfind;
import com.github.gv2011.webdav.methods.DoProppatch;
import com.github.gv2011.webdav.methods.DoPut;
import com.github.gv2011.webdav.methods.DoUnlock;

public class WebDavServletBean extends HttpServlet {

  private static org.slf4j.Logger                LOG         = org.slf4j.LoggerFactory
      .getLogger(WebDavServletBean.class);

  /**
   * MD5 message digest provider.
   */
  protected static MessageDigest                 MD5_HELPER;

  /**
   * The MD5 helper object for this class.
   */
  protected static final MD5Encoder              MD5_ENCODER = new MD5Encoder();

  private static final boolean                   READ_ONLY   = false;
  protected ResourceLocks                        _resLocks;
  protected IWebdavStore                         _store;
  private final HashMap<String, IMethodExecutor> _methodMap  = new HashMap<>();

  public WebDavServletBean() {
    _resLocks = new ResourceLocks();

    try {
      MD5_HELPER = MessageDigest.getInstance("MD5");
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException();
    }
  }

  public void init(final IWebdavStore store, final String dftIndexFile,
      final String insteadOf404, final int nocontentLenghHeaders,
      final boolean lazyFolderCreationOnPut) throws ServletException {

    _store = store;

    final IMimeTyper mimeTyper = (transaction, path) -> {
      String retVal = _store.getStoredObject(transaction, path).getMimeType();
      if (retVal == null) {
        retVal = getServletContext().getMimeType(path);
      }
      return retVal;
    };

    register("GET", new DoGet(store, dftIndexFile, insteadOf404, _resLocks,
        mimeTyper, nocontentLenghHeaders));
    register("HEAD", new DoHead(store, dftIndexFile, insteadOf404,
        _resLocks, mimeTyper, nocontentLenghHeaders));
    final DoDelete doDelete = (DoDelete) register("DELETE", new DoDelete(store,
        _resLocks, READ_ONLY));
    final DoCopy doCopy = (DoCopy) register("COPY", new DoCopy(store, _resLocks,
        doDelete, READ_ONLY));
    register("LOCK", new DoLock(store, _resLocks, READ_ONLY));
    register("UNLOCK", new DoUnlock(store, _resLocks, READ_ONLY));
    register("MOVE", new DoMove(_resLocks, doDelete, doCopy, READ_ONLY));
    register("MKCOL", new DoMkcol(store, _resLocks, READ_ONLY));
    register("OPTIONS", new DoOptions(store, _resLocks));
    register("PUT", new DoPut(store, _resLocks, READ_ONLY,
        lazyFolderCreationOnPut));
    register("PROPFIND", new DoPropfind(store, _resLocks, mimeTyper));
    register("PROPPATCH", new DoProppatch(store, _resLocks, READ_ONLY));
    register("*NO*IMPL*", new DoNotImplemented(READ_ONLY));
  }

  @Override
  public void destroy() {
    if (_store != null)
      _store.destroy();
    super.destroy();
  }

  protected IMethodExecutor register(final String methodName, final IMethodExecutor method) {
    _methodMap.put(methodName, method);
    return method;
  }

  /**
   * Handles the special WebDAV methods.
   */
  @Override
  protected void service(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {

    final String methodName = req.getMethod();
    ITransaction transaction = null;
    boolean needRollback = false;

    if (LOG.isTraceEnabled())
      debugRequest(methodName, req);

    try {
      final Principal userPrincipal = getUserPrincipal(req);
      transaction = _store.begin(userPrincipal);
      needRollback = true;
      _store.checkAuthentication(transaction);
      resp.setStatus(WebdavStatus.SC_OK);

      try {
        IMethodExecutor methodExecutor = _methodMap
            .get(methodName);
        if (methodExecutor == null) {
          methodExecutor = _methodMap
              .get("*NO*IMPL*");
        }

        methodExecutor.execute(transaction, req, resp);

        _store.commit(transaction);
        /**
         * Clear not consumed data
         *
         * Clear input stream if available otherwise later access include
         * current input. These cases occure if the client sends a request with
         * body to an not existing resource.
         */
        if (req.getContentLength() != 0 && req.getInputStream().available() > 0) {
          if (LOG.isTraceEnabled()) {
            LOG.trace("Clear not consumed data!");
          }
          while (req.getInputStream().available() > 0) {
            req.getInputStream().read();
          }
        }
        needRollback = false;
      } catch (final IOException e) {
        final java.io.StringWriter sw = new java.io.StringWriter();
        final java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        LOG.error("IOException: " + sw.toString());
        resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
        _store.rollback(transaction);
        throw new ServletException(e);
      }

    } catch (final UnauthenticatedException e) {
      resp.sendError(WebdavStatus.SC_FORBIDDEN);
    } catch (final WebdavException e) {
      final java.io.StringWriter sw = new java.io.StringWriter();
      final java.io.PrintWriter pw = new java.io.PrintWriter(sw);
      e.printStackTrace(pw);
      LOG.error("WebdavException: " + sw.toString());
      throw new ServletException(e);
    } catch (final Exception e) {
      final java.io.StringWriter sw = new java.io.StringWriter();
      final java.io.PrintWriter pw = new java.io.PrintWriter(sw);
      e.printStackTrace(pw);
      LOG.error("Exception: " + sw.toString());
    } finally {
      if (needRollback)
        _store.rollback(transaction);
    }

  }

  /**
   * Method that permit to customize the way user information are extracted from
   * the request, default use JAAS
   *
   * @param req
   * @return
   */
  protected Principal getUserPrincipal(final HttpServletRequest req) {
    return req.getUserPrincipal();
  }

  private void debugRequest(final String methodName, final HttpServletRequest req) {
    LOG.trace("-----------");
    LOG.trace("WebdavServlet\n request: methodName = " + methodName);
    LOG.trace("time: " + System.currentTimeMillis());
    LOG.trace("path: " + req.getRequestURI());
    LOG.trace("-----------");
    Enumeration<?> e = req.getHeaderNames();
    while (e.hasMoreElements()) {
      final String s = (String) e.nextElement();
      LOG.trace("header: " + s + " " + req.getHeader(s));
    }
    e = req.getAttributeNames();
    while (e.hasMoreElements()) {
      final String s = (String) e.nextElement();
      LOG.trace("attribute: " + s + " " + req.getAttribute(s));
    }
    e = req.getParameterNames();
    while (e.hasMoreElements()) {
      final String s = (String) e.nextElement();
      LOG.trace("parameter: " + s + " " + req.getParameter(s));
    }
  }

}
