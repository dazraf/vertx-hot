package io.dazraf.vertx.compiler;

import io.dazraf.vertx.HotDeployParameters;


public interface Compiler {

  CompileResult compile(HotDeployParameters params) throws Exception;

}
