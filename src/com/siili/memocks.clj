(ns com.siili.memocks
  (:require [clojure.test :as t]))

(defn mock
  "Creates mock function that accepts variable number of arguments
  and will return nil or given value and record all arguments passed
  to the function. If you specify function as a value, it will be
  executed with vector of all invokations as an argument."
  ([]
   (mock nil))
  ([f-or-res]
   (let [a (atom [])
         f (if (fn? f-or-res) f-or-res (constantly f-or-res))]
     (with-meta (fn [& args]
                  (f (swap! a conj args))) {:args a}))))

(defn all-args
  "Returns all recorded arguments."
  [m]
  @(:args (meta m)))

(defn last-args
  "Returns last recorder arguments."
  [m]
  (last (all-args m)))

(defn not-invoked?
  "Checks if mock was never invoked."
  [m]
  (empty? (all-args m)))

(defn invoked?
  "Checks if mock was ever invoked."
  [m]
  (not (not-invoked? m)))

(defn invoked-with?
  "Checks if mock was invoked with given args"
  [m & expected-args]
  (some #(= expected-args %) (all-args m)))

(defmacro with-mocks
  "This macro is a syntactic sugar for mocking specified symbols.

  (with-mocks [somens/somefn x]
    (println (somens/somefn 10)))

  will be replaced with

  (with-redefs [somens/somefn (siili.memocks/mock x)]
    (println (somens/somefn 10))) "
  [mocks & body]
  (assert (even? (count mocks)) "bindings should be vector with even elements")
  (let [pairs (partition 2 mocks)
        bindings (reduce (fn [sum [s v]] (conj sum s `(com.siili.memocks/mock ~v))) [] pairs)]
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
