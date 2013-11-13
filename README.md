# CLJSFiddle

[cljsfiddle.net](http://cljsfiddle.net)

## Local install

```
$ git clone https://github.com/jonase/cljsfiddle
$ cd cljsfiddle
$ export DATOMIC_URI=datomic:free://localhost:4334/cljsfiddle
$ # Note. registered for localhost:8080
$ export GITHUB_CLIENT_ID=d220094711eae05f92ee
$ export GITHUB_CLIENT_SECRET=9514e83d9157e01b3962c162d6e50c0ad3a9b00d
$ export CLJSFIDDLE_VERSION=2
$ # Note. e.x. SESSION_SECRET=1234567891234567
$ export SESSION_SECRET=[16 random characters]
$ wget http://downloads.datomic.com/0.8.4159/datomic-free-0.8.4159.zip
$ unzip datomic-free-0.8.4159.zip
```

open another terminal and start datomic:

```
$ cd datomic-free-0.8.4159
$ bin/transactor config/samples/free-transactor-template.properties 
```

back to the previous terminal:

```
$ lein repl
user=> (require 'cljsfiddle.import)
user=> (in-ns 'cljsfiddle.import)
cljsfiddle.import=> (d/create-database (env :datomic-uri))
cljsfiddle.import=> (def conn (d/connect (env :datomic-uri)))
cljsfiddle.import=> @(d/transact conn schema)
cljsfiddle.import=> ;; Quit the repl
$ lein run -m cljsfiddle.import datomic:free://localhost:4334/cljsfiddle
$ lein cljsbuild once
$ mkdir -p resources/jscache/2
$ lein run -m cljsfiddle.handler
```

Go to http://localhost:8080. Have fun!

## License

Copyright © 2013 Jonas Enlund

Distributed under the Eclipse Public License, the same as Clojure.
