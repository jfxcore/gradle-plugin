# JFXcore Gradle Plugin

Simplifies working with JFXcore for gradle projects.

## Getting started

To use the plugin, apply the following two steps:

### 1. Apply the plugin

##### Using the `plugins` DSL:

**Groovy**

    plugins {
        id 'org.jfxcore.javafxplugin' version '18'
    }

**Kotlin**

    plugins {
        id("org.jfxcore.javafxplugin") version "18"
    }

##### Alternatively, you can use the `buildscript` DSL:

**Groovy**

    buildscript {
        repositories {
            maven {
                url "https://plugins.gradle.org/m2/"
            }
        }
        dependencies {
            classpath 'org.jfxcore:gradle-plugin:18'
        }
    }
    apply plugin: 'org.jfxcore.javafxplugin'

**Kotlin**

    buildscript {
        repositories {
            maven {
                setUrl("https://plugins.gradle.org/m2/")
            }
        }
        dependencies {
            classpath("org.jfxcore:gradle-plugin:18")
        }
    }
    apply(plugin = "org.jfxcore.javafxplugin")


### 2. Specify JFXcore modules

Specify all the JFXcore modules that your project uses:

**Groovy**

    javafx {
        modules = [ 'javafx.controls', 'javafx.fxml' ]
    }

**Kotlin**

    javafx {
        modules("javafx.controls", "javafx.fxml")
    }

### 3. Cross-platform projects and libraries

JFXcore modules require native binaries for each platform. The plugin only
includes binaries for the platform running the build. By declaring the 
dependency configuration **compileOnly**, the native binaries will not be 
included. You will need to provide those separately during deployment for 
each target platform.

**Groovy**

    javafx {
        modules = [ 'javafx.controls', 'javafx.fxml' ]
        configuration = 'compileOnly'
    }

**Kotlin**

    javafx {
        modules("javafx.controls", "javafx.fxml")
        configuration = "compileOnly"
    }

### 4. Using a local JFXcore SDK

By default, JFXcore modules are retrieved from Maven Central. 
However, a local JFXcore SDK can be used instead, for instance in the case of 
a custom build of JFXcore.

Setting a valid path to the local JFXcore SDK will take precedence:

**Groovy**

    javafx {
        sdk = '/path/to/javafx-sdk'
        modules = [ 'javafx.controls', 'javafx.fxml' ]
    }

**Kotlin**

    javafx {
        sdk = "/path/to/javafx-sdk"
        modules("javafx.controls", "javafx.fxml")
    }
