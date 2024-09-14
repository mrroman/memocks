(ns com.siili.memocks-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.siili.memocks :as memocks]))

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

(deftest with-mocks-test
  (testing "mocking symbols"
    (memocks/with-mocks [test-func :mock]
      (is (= :mock (test-func)))))
  (testing "mocks only inside block"
    (memocks/with-mocks [test-func :mock]
      (is (= :mock (test-func))))
    (is (= :original (test-func)))))
