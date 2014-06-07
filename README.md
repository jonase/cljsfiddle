# CLJSFiddle

[cljsfiddle.net](http://cljsfiddle.net)

## Local install

```
$ git clone https://github.com/jonase/cljsfiddle
$ cd cljsfiddle
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
