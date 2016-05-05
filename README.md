[![Stories in Ready](https://badge.waffle.io/mnuessler/influxdb-clojure.png?label=ready&title=Ready)](https://waffle.io/mnuessler/influxdb-clojure)
# InfluxDB Clojure

A minimalistic [InfluxDB][influxdb] client for Clojure, implemented as
a wrapper around the [Java InfluxDB client][influxdb-java]. Compatible
with InfluxDB >= 0.9.

If you are lookig for officially supported InfluxDB clients please
refer to [this list][clients].

## Artifacts

Artifacts are [released to Clojars][clojars].

If you are using Maven, add the following repository definition to your `pom.xml`:

```xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

### The Most Recent Release

With Leiningen:

[![Clojars Project](https://img.shields.io/clojars/v/influxdb/influxdb-clojure.svg)][clojars]

With Maven:

```xml
<dependency>
  <groupId>influxdb</groupId>
  <artifactId>influxdb-clojure</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Usage

### Write points

Require the `influxdb-clojure` namespace:

```clj
(require '[influxdb-clojure.core :as influxdb])
```

Open a connection:

```clj
(def conn (influxdb/connect "http://localhost:8086" "root", "root"))
```

Create a database:

```clj
(influxdb/create-database conn "mydb")
```

Show existing databases:

```clj
(influxdb/databases conn)
=> ("_internal" "mydb")
```

Delete a database:

```clj
(influxdb/delete-database conn "mydb")
(influxdb/databases conn)
=> ("_internal")
```

Write a point to a database (retention policy "default"):

```clj
(def point {:measurement "cpu_load_short"
            :fields {:value 0.64}
            :time 1462428815668
            :tags {:host "server01"
                   :region "us-west"}})
(influxdb/write-points conn "mydb" [point])
```

(The `time` is optional and will be set to the current system time if
not explicitly set.)

Optionally, retention policy and consistency level may be specified:

```clj
(def opts {:retention-policy "six_month_rollup", :consistency-level :quorum}
(influxdb/write-points conn "mydb" [point] opts)
```

## License

Copyright © 2016 Matthias Nüßler

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[clients]: https://docs.influxdata.com/influxdb/v0.12/clients/api/
[clojars]: https://clojars.org/influxdb/influxdb-clojure
[influxdb]: https://influxdata.com/time-series-platform/influxdb/
[influxdb-java]: https://github.com/influxdata/influxdb-java
