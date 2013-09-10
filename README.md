nbm-maven
=========

For of the codehaus nbm-maven repository --> http://svn.codehaus.org/mojo/trunk/mojo/nbm-maven

This fork mainly exists to add missing features to the webstart goal of the org.codehaus.mojo:nbm-maven-plugin.

(Releases will be available through Maven Central)

# nbm-maven-plugin

nbm-maven-plugin home --> http://mojo.codehaus.org/nbm-maven/nbm-maven-plugin

## Features

### Webstart

* Multithreaded jar signing (nbm.signing.threads);
* TSA signing - time based signing (nbm.signing.tsacert, nbm.signing.tsaurl, nbm.signing.retryCount);
* Remove existing signatures (nbm.signing.removeExistingSignatures);
* Webapp resources;
* Able to sign war archive;
* Fix regression: As of Java 6 > release 31 (applies to Java 7), the JDK sample directory doesn't exist anymore. The nbm maven plugin uses the jnlp servlet of sample to bootstrap the application. It is hard coded. The servlet is now part of the plugin;
* Support for m2e lifecycle mapping;
* More robust jar signing to fix edge cases.

## Usage

```xml
    <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>nbm-maven-plugin</artifactId>
        <version>3.11-bitstrings-1</version>
        <extensions>true</extensions>
    </plugin>
```
