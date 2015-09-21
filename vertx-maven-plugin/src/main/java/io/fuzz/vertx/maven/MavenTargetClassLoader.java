package io.fuzz.vertx.maven;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLClassLoader;

public class MavenTargetClassLoader extends URLClassLoader {

  public static ClassLoader create(Void trigger) {
    return new MavenTargetClassLoader();
  }

  public MavenTargetClassLoader() {
    this((URLClassLoader) Thread.currentThread().getContextClassLoader());
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
