(ns com.siili.memocks
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :as t]))

(defn mock
  "Creates a mock function that accepts variable number of arguments
   and will return nil or given value and record all arguments passed
   to the function. If you specify function as a value, it will be
   executed with vector of all calls as an argument."
  ([]
   (mock nil))
  ([f-or-res]
   (let [a (atom [])
         f (if (fn? f-or-res) f-or-res (constantly f-or-res))]
     (with-meta (fn [& args]
                  (f (swap! a conj args))) {:args a}))))

(defn all-args
  "Returns all recorded calls."
  [m]
  @(:args (meta m)))

(defn last-args
  "Returns the last recorded call's arguments."
  [m]
  (last (all-args m)))

(defn not-invoked?
  "Checks if mock was never invoked."
  [m]
  (empty? (all-args m)))

(defn invoked?
  "Checks if mock was invoked at least once, or N times."
  ([m] (not (not-invoked? m)))
  ([m n] (= n (count (all-args m)))))

(defn invoked-with?
  "Checks if mock was invoked with given args"
  [m & expected-args]
  (some #(= expected-args %) (all-args m)))

(defmacro invoked-as?
  "Checks if given mock invocations were present."
  [[m & args]]
  `(invoked-with? ~m ~@args))

(defn- malli-instrument [sym f]
  (try
    (require 'malli.core)
    (let [function-schemas (find-var 'malli.core/function-schemas)
          -instrument (find-var 'malli.core/-instrument)]
      (if-let [schema (get-in (function-schemas) [(symbol (namespace sym))
                                                  (symbol (name sym))])]
        (-instrument schema f)
        f))
    (catch java.io.FileNotFoundException _
      f)))

(defn- clojure-spec-instrument [sym f]
  (if-let [s (s/get-spec sym)]
    (fn [& args]
      (when (= ::s/invalid (s/conform (:args s) args))
        (throw (ex-info (str "Function " sym " arguments don't conform spec")
                        {:explain (s/explain-str (:args s) args)})))
      (let [res (apply f args)]
        (when (= ::s/invalid (s/conform (:ret s) res))
          (throw (ex-info  (str "Function " sym " returned value not conforming ret spec")
                           {:explain (s/explain-str (:ret s) res)})))
        res))
    f))

(defn fn-mock
  "Creates a mock of a function specified by symbol. It uses `mock` function
   to generate a mock. It resolves aliases for namespaces according to current namespace."
  [fn-symbol f-or-res]
  (->> (mock f-or-res)
       (malli-instrument fn-symbol)
       (clojure-spec-instrument fn-symbol)))

(defn- resolve-ns-alias [ns-sym]
  (or (find-ns ns-sym)
      (.lookupAlias *ns* ns-sym)))

(defn- resolve-symbol [fn-sym]
  (let [ns-sym (ns-name (or (some->> fn-sym
                                     namespace
                                     symbol
                                     resolve-ns-alias)
                            *ns*))]
    (symbol (name ns-sym) (name fn-sym))))

(defmacro with-mocks
  "This macro is a syntactic sugar for mocking specified symbols.

  (with-mocks [somens/somefn x]
    (println (somens/somefn 10)))

  will be replaced with

  (with-redefs [somens/somefn (siili.memocks/fn-mock 'somens/fn-mock x)]
    (println (somens/somefn 10))) "
  [mocks & body]
  (assert (even? (count mocks)) "bindings should be vector with even elements")
  (let [pairs (partition 2 mocks)
        bindings (reduce (fn [sum [fn-sym v]]
                           (conj sum fn-sym
                                 (list `fn-mock (list `quote (resolve-symbol fn-sym)) v)))
                         []
                         pairs)]
    `(with-redefs ~bindings
       ~@body)))

(defmethod t/assert-expr 'not-invoked? [msg form]
  ;; Test if x wasn't invoked
  `(let [m# ~(second form)]
     (let [result# ~form]
       (if result#
         (t/do-report {:type :pass, :message ~msg
                       :expected '~form, :actual (all-args m#)})
         (t/do-report {:type :fail, :message ~msg
                       :expected '~form, :actual (map #(cons '~(second form) %) (all-args m#))}))
       result#)))

(defmethod t/assert-expr 'invoked? [msg form]
  ;; Test if x was invoked
  `(let [m# ~(second form)]
     (let [result# ~form]
       (if result#
         (t/do-report {:type :pass, :message ~msg
                       :expected '~form, :actual (all-args m#)})
         (t/do-report {:type :fail, :message ~msg
                       :expected '~form, :actual (map #(cons '~(second form) %) (all-args m#))}))
       result#)))

(defmethod t/assert-expr 'invoked-with? [msg form]
  ;; Test if x was invoked with given args
  `(let [m# ~(second form)]
     (let [result# ~form]
       (if result#
         (t/do-report {:type :pass, :message ~msg
                       :expected '~form, :actual (all-args m#)})
         (t/do-report {:type :fail, :message ~msg
                       :expected '~form, :actual (map #(cons '~(second form) %) (all-args m#))}))
       result#)))
