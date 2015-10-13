# Vert.x-Hot
### A Maven plugin for hot reload of Maven Vert.x applications
---
## Background

I love coding with [Vert.x](http://vertx.io). Truly an incredible toolkit for developing high-performance applications. Being an old-school Maven head, I wanted to make the development of Maven Vert.x Verticles easier. Specifically the ability to iteratively develop and see ones changes automatically reloaded into a fully debuggable JVM.

This plugin is being shared here in the hope that you find it useful also. 

Contributions most gratefully received and recognised.
 
## Aims

1. __Detect__ source changes
2. __```compile```__ stale targets
3. __Hot Reload__
4. __Full Debug__ without needing to attach to secondary processes.
5. __Intuitive__ integration with Maven toolchain
6. __Fast__ at least much faster than using a manual workflow

## Instructions

### Step 1: Download
Whilst the plugin is awaiting upload to Maven Central, clone this project locally and run ```mvn install```.

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

Or, in your favourite IDE: 

* __IntelliJ IDEA__: 
  * *Run* - open the Maven side-bar, *expand* the ```Plugins/vertx``` section and *double-click* on ```vertx:hot``` goal. Any changes to your project's main source (*e.g.* ```src/main```) will cause a hot deploy. 
  * *Debug* - *right-click* on the ```vertx:hot``` goal and *select* ```Debug```.
* __Eclipse__: *This plugin has not been tested in Eclipse as yet.*

### Step 4: Stopping the plugin

Press either: ```<Enter>``` or  ```Ctrl-C```.

## Sample code
There is a simple test project under ```test-vtx-service```.

## Design Notes

![sequence diagram](design.png)

