package com.github.gv2011.webdav.teststore;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.github.gv2011.webdav.WebdavServlet;

public class ManualTest {

  public static void main(final String[] args) throws Exception {
    final WebdavServlet ws = new WebdavServlet();
    final Server s = new Server(8080);
    final ServletHandler handler = new ServletHandler();
    final ServletHolder holder = new ServletHolder();
    holder.setInitParameter("rootpath", ".");
    holder.setName("webdav");
    holder.setServlet(ws);
    handler.addServletWithMapping(holder, "/*");
    s.setHandler(handler);
    s.start();
    s.join();
  }

}
