(ns com.siili.memocks-test
  (:require [clojure.test :refer :all]
            [com.siili.memocks :refer :all]))

(deftest mocking-test
  (testing "mock that returns nil"
    (is (nil? ((mock)))))
  (testing "mock that returns given value"
    (let [m (mock 4)]
      (is (every? #(= 4 %) (map m (range 1000))))))
  (testing "args are recorded"
    (let [m (mock)
          invoked-args (map #(list % (inc %) (dec %)) (range 1000))]
      (doseq [args invoked-args] (apply m args))
      (is (= invoked-args (all-args m)))))
  (testing "mock was not invoked"
    (let [m1 (mock)
          m2 (mock)]
      (m2)
      (is (not-invoked? m1))
      (is (not (not-invoked? m2)))))
  (testing "mock was invoked"
    (let [m1 (mock)
          m2 (mock)]
      (m1 1)
      (is (invoked? m1))
      (is (not (invoked? m2)))))
  (testing "mock was invoked with given args"
    (let [m (mock)]
      (m 3 4 5)
      (m 1 2 3)
      (is (invoked-with? m 1 2 3))
      (is (invoked-with? m 3 4 5))
      (is (not (invoked-with? m 0 0 0)))))
  (testing "mock incokation was present"
    (let [m (mock)]
      (m 3 4 5)
      (m 1 2 3)
      (is (invoked-as? (m 1 2 3)))
      (is (invoked-as? (m 3 4 5)))
      (is (not (invoked-as? (m 0 0 0))))))
  (testing "mock with custom function taking vector of all passed args"
    (let [m (mock (fn [args]
                    (count args)))]
      (m)
      (m 1)
      (m 2)
      (m 3)
      (is (= (m 4) 5))
      (is (= [nil '(1) '(2) '(3) '(4)] (all-args m))))))

(defn test-func []
  :original)

(deftest with-mocks-test
  (testing "mocking symbols"
    (with-mocks [test-func :mock]
      (is (= :mock (test-func)))))
  (testing "mocks only inside block"
    (with-mocks [test-func :mock]
      (is (= :mock (test-func))))
    (is (= :original (test-func)))))
