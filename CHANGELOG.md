3.13.2-2
--------

### Goal `nbm:webstart-app`

 * Add main="true" for startup.jar in default jnlp (master.jnlp).


3.13.2
------

### Global

 * Sync with codehaus 3.13.2;
 * Add org.codehaus.mojo flavor, since NetBeans exclusively interacts with this groupId.


3.11.1-4
--------

### Goal `nbm:webstart-app`

 * New `signingMaximumThreads` paramter.


3.11.1-3
--------

### Goal `nbm:webstart-app`

 * Sign jnlp-servlet.jar;
 * Some fixes.


3.11.1-2
--------

### Goal `nbm:webstart-app`

 * New `verifyJnlp` parameter;
 * New `validateJnlpDtd` parameter.


3.11.1-1
--------

### Goal `nbm:webstart-app`

 * Add `Application-Name` manifest attribute (security);
 * Add `Application-Library-Allowable-Codebase` manifest attribute (security);
 * Add `Caller-Allowable-Codebase` manifest attribute (security);
 * Pack200.


3.11.1
------

### Goal: webstart-app

 * New `autoManifestSecurityEntries` - Automatically generate security entries for each signed jar based on the master JNLP;
 * New `<jarsConfigs>` - Brings per Jar configuration;
 * A bit less IO (just a bit);
 * More compact signing output.


3.11 (Initial Release)
----

### Goal: webstart-app

 * Multi-threaded jar signing (nbm.signing.threads);
 * TSA signing - time based signing (nbm.signing.tsacert, nbm.signing.tsaurl, nbm.signing.retryCount);
 * Remove existing signatures (nbm.signing.removeExistingSignatures);
 * JNLP-INF/APPLICATION_TEMPLATE.JNLP support;
 * Webapp resources;
 * Able to sign war archive;
 * Fix regression: As of Java 6 > release 31 (applies to Java 7), the JDK sample directory doesn't exist anymore. The nbm maven plugin uses the jnlp servlet of sample to bootstrap the application. It is hard coded. The servlet is now part of the plugin;
 * Support for m2e lifecycle mapping;
 * More robust jar signing to fix edge cases.
