package io.dazraf.vertx.deployer;

import io.vertx.core.json.JsonObject;
import rx.functions.Action1;

import java.io.Closeable;
import java.util.List;

public interface VerticleDeployer extends Closeable {

    Closeable deploy(List<String> classPaths) throws Throwable;

    void close();

    Action1<JsonObject> createStatusConsumer();

}
