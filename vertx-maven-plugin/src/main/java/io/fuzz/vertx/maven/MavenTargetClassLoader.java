package io.fuzz.vertx.maven;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

public class MavenTargetClassLoader extends URLClassLoader {
  private static final Logger logger = LoggerFactory.getLogger(MavenTargetClassLoader.class);

  public static URLClassLoader create(URLClassLoader parentClassLoader) {
    return new MavenTargetClassLoader(parentClassLoader);
  }

  public MavenTargetClassLoader(URLClassLoader urlClassLoader) {
    super(urlClassLoader.getURLs(), urlClassLoader);
  }


  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    return findClass(name);
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    try {
      byte[] bytes = loadClassData(name);
      return defineClass(name, bytes, 0, bytes.length);
    } catch (IOException ioe) {
      try {
        return super.loadClass(name, true);
      } catch (ClassNotFoundException ignore) {
      }
      ioe.printStackTrace(System.out);
      return null;
    }
  }

  @Override
  public URL getResource(String name) {
    String path = Utils.getCWD() + "/target/classes/" + name;
    File f = new File(path);
    try {
      return f.toURI().toURL();
    } catch (MalformedURLException e) {
      logger.error("cannot get resource: " + path);
      throw new RuntimeException(e);
    }
  }

  private byte[] loadClassData(String className) throws IOException {
    String cwd = Utils.getCWD();
    File f = new File(cwd + "/target/classes/" + className.replaceAll("\\.", "/") + ".class");
    int size = (int) f.length();
    byte buff[] = new byte[size];
    FileInputStream fis = new FileInputStream(f);
    DataInputStream dis = new DataInputStream(fis);
    dis.readFully(buff);
    dis.close();
    return buff;
  }

}
