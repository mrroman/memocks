# memocks 

It's simple mocking library. You can create a mock function that records all calls and check if it was invoked.

- Mocking return value
- [Recording calls](#recording-calls)
- [Checking calls in tests](#checking-calls) with clojure.test assert expressions
- Instrumenting mocks with [malli schema](#malli-support) or [clojure.spec](#clojurespec-support)

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.mrroman/memocks.svg?include_prereleases)](https://clojars.org/org.clojars.mrroman/memocks)

## Install

You can use git or maven as a dependency source:

```
{org.clojars.mrroman/memocks {:mvn/version "0.1.2"}}
```

or

```
{org.clojars.mrroman/memocks 
    {:git/url "https://github.com/mrroman/memocks.git" :git/tag "0.1.2" :git/sha "3859de3ea99bd5795cadeba8f4fba3c5252632b2"}}
```

## Usage

Import memocks:

```clj
(require '[com.siili.memocks :as memocks])
```

### Simple mock

Create new mock function:

```clj
(def m (memocks/mock))
```

### Recorded arguments

You can call it as any other function and check all recorded calls.

```clj

(m :debug "Start process...")
(m :info "Results:")
(m :debug "Stop process...")

(memocks/all-args m)
=> [(:debug "Start process...") (:info "Results:") (:debug "Stop process...")]
```

### Checking calls 

Checking if the call happened can be cumbersome, so I've implemented a couple of predicates:

```clj

(memocks/not-invoked? m)
=> false

(memocks/invoked? m)
=> true

(memocks/invoked-with? m :info "Results:")
=> true

(memocks/invoked-with? m :info "Results:" 3.14159)
=> false

(memocks/invoked-as? (m :info "Results:"))
=> true

(memocks/invoked-as? (m :info "Results:" 3.14159))
=> false
```

> `invoked-as?` looks a bit like `invoked-with?` but has different semantics.
> It will be treated as a function invocation to linters so they can check e.g. if function arity is valid.

> `(memocks/invoked-with?)` and `(memocks/invoked-as?)` use regular `=` operator to compare arguments.

### Mock returning a value

You can create a mock that returns a value:

```clj

(def two (memocks/mock 2))

(two)
=> 2
```

### Mock with a result computing function

You can also create a mock with a custom function that calculates the result.
The function takes as an argument a list with all recorded calls.

```clj

(def stub (memocks/mock (fn [calls] (last calls))))

(stub 1)
=> (1)

(stub 2)
=> (2)

(stub 3)
=> (3)
```

In this example, mock will return the last call (a list of arguments).

### Mocking symbols

Memocks provides a convenient macro that allows you to mock functions easily.
Let's mock function http/get.

```clj
(with-mocks [http/get {:body "OK" :status 200}
             http/post {:body "NOK" :status 500}]
  (http-get)
  (http-post))
```

Memocks uses `with-redefs` macro provided by Clojure.

### Malli support

Memocks supports Malli function schemas. You can create a mock function instrumented
with malli based on schema of a function. E.g.

```clj
(require '[malli.core :as m])

(m/=> my-inc [:=> [:cat :int] :int])
(defn my-inc [x]
  (inc x))

;; You have to provide a symbol of function 
;; or with or without namespace (aliases are supported).
(def my-inc-mock (mock-fn 'my-inc 1))

(my-inc-mock 0)
;=> 1

(my-inc-mock "aa")
;=> An exception ::invalid-input

(def my-inc-mock2 (mock-fn 'my-inc nil))
(my-inc-mock2 1)
;=> An exception ::invalid-output
```

If you use `with-mocks` macro, it will our of the box.

> If you use :malli/schema metadata to define schema, you have to use dev/start!
> to enable mock instrumentation.

### clojure.spec support

Memocks supports clojure.spec function specs. You can create a mock function 
which will check if arguments and return value conforms with spec. E.g.

```clj
(require '[clojure.spec.alpha :as s])

(s/fdef my-inc
  :args (s/cat :x int?)
  :ret int?)
(defn my-inc [x]
  (inc x))

;; You have to provide a symbol of function 
;; or with or without namespace (aliases are supported).
(def my-inc-mock (mock-fn 'my-inc 1))

(my-inc-mock 0)
;=> 1

(my-inc-mock "aa")
;=> An exception invalid arguments

(def my-inc-mock2 (mock-fn 'my-inc nil))
(my-inc-mock2 1)
;=> An exception invalid return value
```

If you use `with-mocks` macro, it will our of the box.

## License

Copyright © 2016 Konrad Mrożek

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
