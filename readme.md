# Vert.x-Hot
### A Maven plugin for hot re-deploy of Maven Vert.x applications
---
## Background

I wrote this plugin because, first and foremost, I love using [Vert.x](http://vertx.io) - it has feature-rich and elegant APIs and outstanding performance. Secondly, I wanted the rapid development model that I've experienced
with frameworks like [Play](https://www.playframework.com/) but, notably, in a traditional *Maven project*.

I use this plugin for my own workflow, and I'm sharing it in case anyone else finds it useful. Contributions most gratefully received and recognised.
 
## Aims

1. Simple integration with Maven.
2. Leverage the maven project definition to correctly identify files that needed rebuilding.
3. Exercise rebuilding all maven outputs in the ```compile``` phase. This includes generated source, resources, objects etc.
4. Much faster iteration cycle than the manual processes carried out within the IDE.
5. __Full Debug__ capability without needing to attach to secondary processes.

## Instructions
Four simple steps: *Download*, *Add*, *Run* and *Stop*.

### Step 1: Download
Until I upload to maven central, clone this project locally and run ```mvn install```.

### Step 2: Add to your project
Add the following to your project ```pom.xml```:

```xml
<plugin>
    <groupId>io.dazraf</groupId>
    <artifactId>vertx-hot-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <verticleClassName>io.dazraf.service.App</verticleClassName>
        <configFile>config.json</configFile>
    </configuration>
</plugin>
```

The ```configuration``` has just two parameters:

* ```verticleClassName``` - the fully qualified class name of your master verticle.
* ```configFile``` - the location of the config file *e.g.* if the config file is in the project ```resources``` root directory as ```config.json``` then ```<configFile>config.json<configFile>```.  

### Step 3: Run it

You can run it either on the command line with:

``` 
mvn vertx:hot
```

Or, in your favourite IDE. In IntelliJ IDEA, I open the Maven side-bar, *expand* the ```Plugins/vertx``` section and 
*double-click* on ```vertx:hot``` goal.

Any changes to your project's main source (*e.g.* ```src/main```) will cause a hot deploy.

If I want to debug, then similar to above, I *right-click* on the ```vertx:hot``` goal and *select* ```Debug```.

### Step 4: Stopping the plugin

Press either: ```<Enter>``` or  ```Ctrl-C```.

## Sample code
There is a simple test project under ```test-vtx-service```.

## Design

```io.dazraf.vertx.maven.plugin.mojo.VertxHotDeploy#execute``` is the entry point. 
This collects the maven project paths, together with the classpaths for all dependencies, and defers to
 ```io.dazraf.vertx.maven.HotDeploy``` for execution. 
 
```HotDeploy``` creates a file watcher on all project paths (sources, resources etc), buffers in chunks to avoid 
excessive rebuilds and defers to a build pipeline in ```HotDeploy#onFileChangeDetected```. This in turn calls the compiler, 
```io.dazraf.vertx.maven.Compiler``` to compile with the maven invoker library and then to ```io.dazraf.vertx.maven.VertxManager``` to create a 
```Closeable``` vert.x instance with the configured verticle and configFile.

```VertxManager``` deploys the verticle in a full [Isolation Group](http://vertx.io/docs/vertx-core/java/#_verticle_isolation_groups).
It returns a ```Closable``` which, when invoked by ```HotDeploy```, tears down your verticle hierarchy.
