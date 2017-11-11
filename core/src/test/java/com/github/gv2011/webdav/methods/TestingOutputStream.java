package com.github.gv2011.webdav.methods;

import static com.github.gv2011.util.ex.Exceptions.notYetImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.junit.Ignore;

@Ignore
public class TestingOutputStream extends ServletOutputStream {

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Override
    public void write(final int i) throws IOException {
        baos.write(i);
    }

    @Override
    public String toString() {
        return baos.toString();
    }

    @Override
    public boolean isReady() {
      // TODO Auto-generated method stub
      throw notYetImplementedException();
    }

    @Override
    public void setWriteListener(final WriteListener writeListener) {
      // TODO Auto-generated method stub
      throw notYetImplementedException();
    }
}
