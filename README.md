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
$ # Note. e.x. SESSION_SECRET=1234567891234567
$ export SESSION_SECRET=[16 random characters]
$ wget https://my.datomic.com/downloads/free/0.9.4815
$ unzip 0.9.4815
```

open another terminal and start datomic:

```
$ cd datomic-free-0.9.4815
$ bin/transactor config/samples/free-transactor-template.properties 
```

back to the previous terminal:

```
$ lein db-create 
$ lein db-assets
$ lein cljsbuild once prod
$ lein run -m cljsfiddle.handler
```

Go to http://localhost:8080. Have fun!

## License

Copyright © 2014 Jonas Enlund

Distributed under the Eclipse Public License, the same as Clojure.
