# memocks [![Build Status](https://travis-ci.org/siilisolutions-pl/memocks.svg?branch=master)](https://travis-ci.org/siilisolutions-pl/memocks) [![Clojars Project](https://img.shields.io/clojars/v/siili/memocks.svg)](https://clojars.org/siili/memocks)

It's simple mocking library. You can create mock function
that records all arguments and check if it was invoked.

## Usage

Import memocks:

```clj
(require '[siili.memocks :as memocks])
```

### Simple mock

Create new mock function:

```clj
(def m (memocks/mock))
```

### Recorded arguments

You can pass it as any other function and check all recorded arguments

```clj

(m :debug "Start process...")
(m :info "Results:")
(m :debug "Stop process...")

(memocks/all-args m)
=> [(:debug "Start process...") (:info "Results:") (:debug "Stop process...")]
```

You can check if function was invoked and if it was invoked with given arguments:

```clj

(memocks/not-invoked? m)
=> false

(memocks/invoked? m)
=> true

(memocks/invoked-with? m :info "Results:")
=> true

(memocks/invoked-with? m :info "Results:" 3.14159)
=> false
```

> `(memocks/invoked-with?)` uses regular `=` operator.

### Mock returning value

You can create mock that returns value:

```clj

(def two (memocks/mock 2))

(two)
=> 2
```

### Mock with a result computing function

You can also create mock with a custom function that calculates result.
The function takes as an argument a list with all provided args.

```clj

(def stub (memocks/mock (fn [x] (last x))))

(stub 1)
=> (1)

(stub 2)
=> (2)

(stub 3)
=> (3)
```

In this example, mock will return a list with args from the last call.

### Mocking symbols

Memocks provides a convienient macro that allows you to mock functions easily.
Lets mock function http/get.

```clj
(with-mocks [http/get {:body "OK" :status 200}
             http/post {:body "NOK" :status 500}]
  (http-get)
  (http-post))
```

Memocks uses `with-redefs` macro provided by Clojure.

## License

Copyright © 2016 Konrad Mrożek

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
