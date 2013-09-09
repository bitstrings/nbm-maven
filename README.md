nbm-maven-plugin
================

Fork of nbm-maven-plugin -> http://mojo.codehaus.org/nbm-maven/nbm-maven-plugin

## Features

### Webstart

* Multithreaded jar signing (nbm.signing.threads);
* TSA signing - time based signing (nbm.signing.tsacert, nbm.signing.tsaurl, nbm.signing.retryCount);
* Remove existing signatures (nbm.signing.removeExistingSignatures);
* Webapp resources;
* Able to sign war archive;
* Fix regression: As of Java 6 > release 31 (applies to Java 7), the JDK sample directory doesn't exist anymore. The nbm maven plugin uses the jnlp servlet of sample to bootstrap the application. It is hard coded. The servlet is now part of the plugin;
* More robust jar signing to fix edge cases.
