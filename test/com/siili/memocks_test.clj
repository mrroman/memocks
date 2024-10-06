(ns com.siili.memocks-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is testing]]
   [com.siili.memocks :as memocks]
   [com.siili.memocks-test :as test-alias]
   [malli.core :as m]))

(deftest mocking-test
  (testing "mock that returns nil"
    (is (nil? ((memocks/mock)))))
  (testing "mock that returns given value"
    (let [m (memocks/mock 4)]
      (is (every? #(= 4 %) (map m (range 1000))))))
  (testing "args are recorded"
    (let [m (memocks/mock)
          invoked-args (map #(list % (inc %) (dec %)) (range 1000))]
      (doseq [args invoked-args] (apply m args))
      (is (= invoked-args (memocks/all-args m)))
      (is (= (last invoked-args) (memocks/last-args m)))))
  (testing "mock was not invoked"
    (let [m1 (memocks/mock)
          m2 (memocks/mock)]
      (m2)
      (is (memocks/not-invoked? m1))
      (is (not (memocks/not-invoked? m2)))))
  (testing "mock was invoked"
    (let [m1 (memocks/mock)
          m2 (memocks/mock)]
      (m1 1)
      (is (memocks/invoked? m1))
      (is (not (memocks/invoked? m2)))))
  (testing "mock was invoked with given args"
    (let [m (memocks/mock)]
      (m 3 4 5)
      (m 1 2 3)
      (is (memocks/invoked-with? m 1 2 3))
      (is (memocks/invoked-with? m 3 4 5))
      (is (not (memocks/invoked-with? m 0 0 0)))))
  (testing "mock incokation was present"
    (let [m (memocks/mock)]
      (m 3 4 5)
      (m 1 2 3)
      (is (memocks/invoked-as? (m 1 2 3)))
      (is (memocks/invoked-as? (m 3 4 5)))
      (is (not (memocks/invoked-as? (m 0 0 0))))))
  (testing "mock with custom function taking vector of all passed args"
    (let [m (memocks/mock (fn [args]
                            (count args)))]
      (m)
      (m 1)
      (m 2)
      (m 3)
      (is (= (m 4) 5))
      (is (= [nil '(1) '(2) '(3) '(4)] (memocks/all-args m))))))

(defn test-func []
  :original)

(m/=> test-func2 [:=> [:cat :int] :int])
(defn test-func2 [x]
  (inc x))

(s/fdef test-func3
  :args (s/cat :x int?)
  :ret int?)
(defn test-func3 [x]
  (inc x))

(s/fdef test-func4
  :args (s/cat :x int?))
(defn test-func4 [x]
  (inc x))

#_{:clj-kondo/ignore [:type-mismatch]}
(deftest with-mocks-test
  (testing "mocking symbols"
    (memocks/with-mocks [test-func :mock]
      (is (= :mock (test-func)))))
  (testing "mocks only inside block"
    (memocks/with-mocks [test-func :mock]
      (is (= :mock (test-func))))
    (is (= :original (test-func))))
  (testing "use malli schema for mocked function"
    (memocks/with-mocks [test-func2 1]
      (is (= 1 (test-func2 1)))
      (testing "invalid input"
        (is (thrown? Exception (test-func2 nil)))))
    (memocks/with-mocks [test-alias/test-func2 1]
      (is (= 1 (test-alias/test-func2 1)))
      (testing "invalid input"
        (is (thrown? Exception (test-alias/test-func2 nil)))))
    (memocks/with-mocks [test-func2 nil]
      (testing "invalid output"
        (is (thrown? Exception (test-func2 1))))))
  (testing "use clojure spec schema for mocked function"
    (memocks/with-mocks [test-func3 1]
      (is (= 1 (test-func3 1)))
      (testing "invalid input"
        (is (thrown? Exception (test-func3 nil)))))
    (memocks/with-mocks [test-alias/test-func3 1]
      (is (= 1 (test-alias/test-func3 1)))
      (testing "invalid input"
        (is (thrown? Exception (test-alias/test-func3 nil)))))
    (memocks/with-mocks [test-func3 nil]
      (testing "invalid output"
        (is (thrown? Exception (test-func3 1)))))
    (memocks/with-mocks [test-func4 nil]
      (testing "missing ret spec"
        (is (= nil (test-func4 2)))))))
