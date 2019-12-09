## Hawkular-Alerts to Quarkus

Feature set:
* CPOL-3/4/5/6/9/10/11, 12/13/14
* Facts are available inside Event.getFacts(), requires modified versions of Alerts 2
* REST-interfaces are no longer from Hawkular-Alerts repository, instead a subset is here ported to newer Vertx.
* UI is provided, but no longer generated from the repo (debugging use only)
* Antlr version 4.6 is no longer compatible with Quarkus (Quarkus pushes 4.7.2)
  * Hawkular-Alerts ANTLR files are not compatible with 4.7.2
* Infinispan version 10.0.0 causes issues, 10.0.0.RC1 works fine (?)

Build Hawkular-Alerts from here: https://github.com/burmanm/hawkular-alerts/tree/core_only

Start Kafka with ``docker run --rm -e RUNTESTS=0 --net=host lensesio/fast-data-dev``

Start clean instance of this with: ``rm -fr target/alerting && ./mvnw quarkus:dev``
