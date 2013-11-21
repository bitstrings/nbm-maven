3.11.1-1
--------

### Goal `nbm:webstart-app`

 * Add `Application-Name` manifest attribute (security); **_DONE_**
 * Add `Application-Library-Allowable-Codebase` manifest attribute (security); **_DONE_**
 * Add `Caller-Allowable-Codebase` manifest attribute (security); **_DONE_**
 * Pack200. **_DONE_**


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
