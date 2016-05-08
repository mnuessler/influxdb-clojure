# InfluxDB Clojure
[![Clojars Project][clojars-shields-badge]][clojars]
[![Stories in Ready][waffle-badge]][waffle-board]

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

[![Clojars Project][clojars-latest-badge]][clojars]

With Maven:

```xml
<dependency>
  <groupId>influxdb</groupId>
  <artifactId>influxdb-clojure</artifactId>
  <version>0.2.0</version>
</dependency>
```

With Gradle:

```groovy
compile "influxdb:influxdb-clojure:0.2.0"
```

## Usage

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

### Writing Data

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
(def opts {:retention-policy "six_month_rollup", :consistency-level :quorum})
(influxdb/write-points conn "mydb" [point] opts)
```

### Querying Data

```clj
(influxdb/query conn "SHOW DATABASES")
=>
{:results [{:series [{:name "databases",
                      :colums ["name"],
                      :values [["_internal"] ["mydb"]]}]}]}
```

```clj
(influxdb/query conn "SELECT * FROM \"cpu_load_short\" WHERE host='server01'" "mydb")
=>
{:results [{:series [{:name "cpu_load_short",
                      :colums ["time" "host" "region" "value"],
                      :values [["2016-05-05T06:06:46.996Z" "server01" "us-west" 0.64]]}]}]}
```

### HTTP Client Configuration

In some cases it may be useful to customize certain HTTP client
parameters such as connect timeout. Timeout parameters can be passed
as part of an option map when the connection is created (in ms):

```clj
(def opts {:connect-timeout 1000
           :read-timeout    5000
           :write-timeout   1000})
(def conn (influxdb/connect "http://localhost:8086" "root", "root" opts))
```

In case additional configuration for the HTTP client is required, a
client instance can be passed instead:

```clj
(import (retrofit.client OkClient)
        (com.squareup.okhttp OkHttpClient)
        (java.util.concurrent TimeUnit))
(def http-client (OkHttpClient.))
(.setConnectTimeout http-client 5000 (TimeUnit/MILLISECONDS))
(.setReadTimeout http-client 5000 (TimeUnit/MILLISECONDS))
(def client (OkClient. http-client))
(def conn (influxdb/connect "http://localhost:8086" "root", "root" {:client client}))
```

## License

Copyright © 2016 Matthias Nüßler

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[clients]: https://docs.influxdata.com/influxdb/v0.12/clients/api/
[clojars]: https://clojars.org/influxdb/influxdb-clojure
[influxdb]: https://influxdata.com/time-series-platform/influxdb/
[influxdb-java]: https://github.com/influxdata/influxdb-java

[clojars-latest-badge]: http://clojars.org/influxdb/influxdb-clojure/latest-version.svg
[clojars-shields-badge]: https://img.shields.io/clojars/v/influxdb/influxdb-clojure.svg
[waffle-badge]: https://badge.waffle.io/mnuessler/influxdb-clojure.png?label=ready&title=Ready
[waffle-board]: https://waffle.io/mnuessler/influxdb-clojure
