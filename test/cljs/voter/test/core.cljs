(ns voter.test.core
  (:require [voter.core :refer [make-local-state participant->map vote-state-key]]
            [cemerick.cljs.test :as t])
  (:require-macros [cemerick.cljs.test :refer [is deftest testing]]))

(defn map->participant [m]
  (js-obj
   "person" (js-obj
             "id" (:id m)
             "displayName" (:name m)
             "image" (js-obj
                      "url" (:img-url m)))))

(deftest test-helper-fns
  (let [m {:id "foo", :name "Quux", :img-url "http://a.bc/d.png"}]
    (is (= m (participant->map (map->participant m))))))

(deftest test-local-state
  (testing "single voter"
    (let [participants [(map->participant {:id "1", :name "Quux", :img-url "http://a.bc/d.png"})]
          my-id "1"]
      (testing "initial state"
        (let [shared-state (js-obj)
              local-state (make-local-state participants shared-state my-id)]
          (is (= my-id (:my-id local-state)))
          (is (= "1" (:id (first (:participants local-state)))))
          (is (= :open (:vote-state local-state)))
          (is (= :none (:transition-type local-state)))))
      (testing "1 vote cast"
        (let [shared-state (js-obj "1" "9")
              local-state (make-local-state participants shared-state my-id)]
          (is (= "9" (:vote (first (:participants local-state)))))
          (is (= :closed (:vote-state local-state)))
          (is (= :reset (:transition-type local-state)))))))
  (testing "two participants"
    (let [participants [(map->participant {:id "1", :name "Quux", :img-url ""})
                        (map->participant {:id "2", :name "Qaaz", :img-url ""})]
          my-id "1"]
      (testing "one vote - no vote-state set"
        (let [shared-state (js-obj "1" "9", "2" "", "-estimation-party-state" "")
              local-state (make-local-state participants shared-state my-id)]
          (is (= :open (:vote-state local-state)))
          (is (= :close (:transition-type local-state)))))
      (testing "one vote - vote state open"
        (let [shared-state (js-obj "1" "9", "2" "", "-estimation-party-state" "open")
              local-state (make-local-state participants shared-state my-id)]
          (is (= :open (:vote-state local-state)))
          (is (= :close (:transition-type local-state)))))
      (testing "one vote - vote state closed"
        (let [shared-state (js-obj "1" "9", "2" "", "-estimation-party-state" "closed")
              local-state (make-local-state participants shared-state my-id)]
          (is (= :closed (:vote-state local-state)))
          (is (= :reset (:transition-type local-state)))))
      (testing "all votes in"
        (let [shared-state (js-obj "1" "9", "2" "8", "-estimation-party-state" "")
              local-state (make-local-state participants shared-state my-id)]
          (is (= :closed (:vote-state local-state)))
          (is (= :reset (:transition-type local-state))))))))
