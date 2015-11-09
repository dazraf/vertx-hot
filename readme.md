# vertx:hot
### A Maven plugin for hot reload of Maven Vert.x projects
---

[![Build Status](https://travis-ci.org/dazraf/vertx-hot.svg?branch=master)](https://travis-ci.org/dazraf/vertx-hot)

## Contents

1. [Background](#background)
2. [Demo](#demo)
3. [Aims](#aims)
4. [Instructions](#instructions)
5. [Example Project](#example-project)
6. [Design Notes](#design-notes)
7. [Contributors](#contributors)

## Background

I love coding with [Vert.x](http://vertx.io). Truly an incredible toolkit for developing high-performance applications. Being an old-school Maven head, I wanted to make the development of Maven Vert.x Verticles easier. Specifically to rapidly develop and see ones changes automatically reloaded into a fully debuggable JVM. 

This plugin was originally written for personal use and is shared here in the hope you find it useful also.

Contributions most gratefully received and recognised.

## Demo
![demo](animation.gif)

## Aims

1. __Detect__ source changes
2. __`compile`__ stale targets
3. __Hot Reload__
4. __Full Debug__ without needing to attach to secondary processes
5. __Intuitive__ integration with Maven toolchain
6. __Fast__ at least much faster than using a manual workflow

## Instructions

### Step 1: Download
Whilst the plugin is awaiting upload to Maven Central, clone this project locally and run `mvn install`.

### Step 2: Add to your project
Add the following to your project `pom.xml`:

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

The `configuration` has just two parameters:

* `verticleClassName` - the fully qualified class name of your master verticle.
* `configFile` - the location of the config file *e.g.* if the config file is in the project `resources` root directory as `config.json` then `<configFile>config.json<configFile>`.  

### Step 3: Run it

You can run it either on the command line with:

```
mvn vertx:hot
```

Or, in your favourite IDE: 

* __For any IDE__ you'll need a locally installed maven installation. Bundled / Embedded maven installations [do not work](https://github.com/dazraf/vertx-hot/issues/3).
* __IntelliJ IDEA__: 
  * *Run* - open the Maven side-bar, *expand* the `Plugins/vertx` section and *double-click* on `vertx:hot` goal. Any changes to your project's main source (*e.g.* `src/main`) will cause a hot deploy. 
  * *Debug* - *right-click* on the `vertx:hot` goal and *select* `Debug`.
* __Eclipse__:
  * *Run* - create maven build runner for `vertx:hot` goal. For Eclipse Mars on OS X, I found I had to set the JAVA_HOME environment variable in the runner. Once setup, `Run` it.
  * *Debug* - as above, but instead of `Run`, `Debug`.

### Step 4: Stopping the plugin

Press either: `<Enter>` or  `Ctrl-C`.

## Example Project
There are two simple test project under `example1` and `example2`. 
The latter is an adaption of the excellent [ToDo App](http://scotch.io/tutorials/javascript/creating-a-single-page-todo-app-with-node-and-angular)
by [Scotch](http://scotch.io), with a vert.x reactive flavour.

To run either: 

1. You will need [bower](http://bower.io) available on your path.
2. After running `mvn clean install` in the parent directory
3. `cd example1` or `cd example2`
4. `mvn vertx:hot`
5. Browse to [http://localhost:8888](http://localhost:8888)
6. Open up the project in your favourite IDE/Editor and try changing some code 
7. Watch the command line for the rebuild and reload into the browser (would be neat if we could auto-reload in the browser ...)

## Design Notes

### Plugin Properties

**Required**

* `verticleClassName` - the fully-qualified reference to the top-level verticle of your application.

**Optional**
 
* `configFile` - the class path to the verticle configuration file. The configuration that is loaded will be decorated 
with the property `devmode` set to `true`.

* `liveHttpReload` - when set to `true`, any web pages served by the application verticles will reload automatically 
  when the application is recompiled or when any static resources are updated. Default is `true`.
  
* `buildResources` - when set to `true`, any change to files under the resource directories will trigger a compile. 
Use this if your resources generate sources. Default is `false`.

* `notificationPort` - the websocket port for notifications to the browser, when used in conjunction with 
`liveHttpReload` set to `true`. Default is `9999`. 
 
### Sequence

![sequence diagram](design.png)

## Contributors

With many thanks:

* [illuminace](https://github.com/illuminace)

