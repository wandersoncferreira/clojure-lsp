(ns clojure-lsp.refactor.edit-test
  (:require
    [clojure-lsp.refactor.edit :as edit]
    [clojure.test :refer :all]
    [rewrite-clj.zip :as z]))

(deftest find-op-test
  (let [code "(foo ((x) [a] (b {c d})))"
        val-finder (fn [v] #(z/find-next-value %1 z/next v))
        op-from-val (fn [finder]
                      (-> code
                          z/of-string
                          finder
                          edit/find-op
                          z/sexpr))]
    (is (= 'foo (op-from-val identity)))
    (is (= 'foo (op-from-val (val-finder 'foo))))
    (is (= '(x) (op-from-val (val-finder 'a))))
    (is (= 'b (op-from-val (val-finder 'd))))
    (is (= 'x (op-from-val (val-finder 'x))))
    (is (= '(x) (op-from-val (comp z/right (val-finder 'foo)))))))

(deftest find-ops-up-test
  (let [code "(foo ((x) [a] (b {c d})))"
        val-finder (fn [v] #(z/find-next-value %1 z/next v))
        op-from-val (fn [finder ops]
                      (-> code
                          z/of-string
                          finder
                          (edit/find-ops-up ops)
                          z/sexpr))]
    (testing "find top op"
      (is (= 'foo (op-from-val (val-finder 'a) 'foo)))
      (is (= 'foo (op-from-val (val-finder 'foo) 'foo)))
      (is (= 'foo (op-from-val (val-finder 'd) 'foo)))
      (is (= 'foo (op-from-val (val-finder 'x) 'foo)))
      (is (= 'foo (op-from-val (comp z/right (val-finder 'foo)) 'foo))))
    (testing "find inner op"
      (is (= nil (op-from-val (val-finder 'a) 'b)))
      (is (= nil (op-from-val (val-finder 'foo) 'b)))
      (is (= nil (op-from-val (val-finder 'x) 'b)))
      (is (= nil (op-from-val (comp z/right (val-finder 'foo)) 'b)))
      (is (= 'b (op-from-val (val-finder 'd) 'b)))
      (is (= 'b (op-from-val (val-finder 'b) 'b)))
      (is (= 'b (op-from-val (comp z/up (val-finder 'b)) 'b))))))

(deftest find-namespace-name
  (testing "without ns on file"
    (let [code "(foo ((x) [a] (b {c d})))"
          zloc (-> code z/of-string (z/find-next-value z/next 'd))]
      (is (= nil
             (edit/find-namespace-name zloc)))))
  (testing "with ns on file"
    (let [code "(ns some.foo.bar (require [some.foo :as s]))\n
                (foo ((x) [a] (b {c d})))"
          zloc (-> code z/of-string (z/find-next-value z/next 'd))]
      (is (= "some.foo.bar"
             (edit/find-namespace-name zloc))))))

(defn assert-function-name [code]
  (let [zloc (-> code z/of-string (z/find-next-value z/next 'd))]
    (is (= "foo"
           (z/string (edit/find-function-name zloc))))))

(deftest find-function-name
  (testing "defn"
    (testing "simple" (assert-function-name "(defn foo [] (let [a 1] d))"))
    (testing "with map" (assert-function-name "(defn {:asd :ds} foo [] (let [a 1] d))"))
    (testing "with meta map" (assert-function-name "(defn ^{:asd :ds} foo [] (let [a 1] d))"))
    (testing "with meta" (assert-function-name "(defn ^:private foo [] (let [a 1] d))")))
  (testing "defn-"
    (testing "simple" (assert-function-name "(defn- foo [] (let [a 1] d))"))
    (testing "with map" (assert-function-name "(defn- {:asd :ds} foo [] (let [a 1] d))"))
    (testing "with meta map" (assert-function-name "(defn- ^{:asd :ds} foo [] (let [a 1] d))"))
    (testing "with meta" (assert-function-name "(defn- ^:private foo [] (let [a 1] d))")))
  (testing "def"
    (testing "simple" (assert-function-name "(def foo (let [a 1] d))"))
    (testing "with map" (assert-function-name "(def {:asd :ds} foo (let [a 1] d))"))
    (testing "with meta map" (assert-function-name "(def ^{:asd :ds} foo (let [a 1] d))"))
    (testing "with meta" (assert-function-name "(def ^:private foo (let [a 1] d))")))
  (testing "defmacro"
    (testing "simple" (assert-function-name "(defmacro foo [] (let [a 1] d))"))
    (testing "with map" (assert-function-name "(defmacro {:asd :ds} foo [] (let [a 1] d))"))
    (testing "with meta map" (assert-function-name "(defmacro ^{:asd :ds} foo [] (let [a 1] d))"))
    (testing "with meta" (assert-function-name "(defmacro ^:private foo [] (let [a 1] d))")))
  (testing "defmulti"
    (testing "simple" (assert-function-name "(defmulti foo [] (let [a 1] d))"))
    (testing "with map" (assert-function-name "(defmulti {:asd :ds} foo [] (let [a 1] d))"))
    (testing "with meta map" (assert-function-name "(defmulti ^{:asd :ds} foo [] (let [a 1] d))"))
    (testing "with meta" (assert-function-name "(defmulti ^:private foo [] (let [a 1] d))")))
  (testing "defonce"
    (testing "simple" (assert-function-name "(defonce foo (let [a 1] d))"))
    (testing "with map" (assert-function-name "(defonce {:asd :ds} foo (let [a 1] d))"))
    (testing "with meta map" (assert-function-name "(defonce ^{:asd :ds} foo (let [a 1] d))"))
    (testing "with meta" (assert-function-name "(defonce ^:private foo (let [a 1] d))")))
  (testing "s/def"
    (testing "simple" (assert-function-name "(s/def foo :- s/String (let [a 1] d))"))
    (testing "with map" (assert-function-name "(s/def {:asd :ds} foo :- s/String (let [a 1] d))"))
    (testing "with meta map" (assert-function-name "(s/def ^{:asd :ds} foo :- s/String (let [a 1] d))"))
    (testing "with meta" (assert-function-name "(s/def ^:private foo :- s/String (let [a 1] d))")))
  (testing "s/defn"
    (testing "simple" (assert-function-name "(s/defn foo :- s/String [] (let [a 1] d))"))
    (testing "with map" (assert-function-name "(s/defn {:asd :ds} foo :- s/String [] (let [a 1] d))"))
    (testing "with meta map" (assert-function-name "(s/defn ^{:asd :ds} foo :- s/String [] (let [a 1] d))"))
    (testing "with meta" (assert-function-name "(s/defn ^:private foo :- s/String [] (let [a 1] d))"))))