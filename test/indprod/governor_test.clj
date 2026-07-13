(ns indprod.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [indprod.store :as store]
            [indprod.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Trade"})
    (store/register-process! st {:process-id "P-1" :client-id "client-1"
                                 :name "shaft-turning" :lsl 9.95 :usl 10.05
                                 :allowable-stack 0.30})
    st))

(defn- measure [v]
  {:op :approve-measurement :effect :propose :process-id "P-1" :measured v
   :confidence 0.9 :stake :low})

(defn- stackup [v]
  {:op :approve-stackup :effect :propose :process-id "P-1" :stack v
   :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-tolerance-band
  (let [st (fresh-store)
        v (governor/check req {} (measure 10.0) st)]
    (is (:ok? v))))

(deftest ok-at-exact-band-edges
  (testing "the band boundary is inclusive"
    (let [st (fresh-store)]
      (is (:ok? (governor/check req {} (measure 9.95) st)))
      (is (:ok? (governor/check req {} (measure 10.05) st))))))

(deftest hard-on-below-lsl
  (testing "interval containment is arithmetic, not judgement"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (measure 9.90) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :out-of-tolerance (:rule %)) (:violations v))))))

(deftest hard-on-above-usl
  (let [st (fresh-store)
        v (governor/check req {} (assoc (measure 10.10) :confidence 0.99) st)]
    (is (:hard? v))
    (is (some #(= :out-of-tolerance (:rule %)) (:violations v)))))

(deftest ok-stackup-within-allowable
  (let [st (fresh-store)
        v (governor/check req {} (stackup 0.25) st)]
    (is (:ok? v))))

(deftest hard-on-stackup-exceeding-allowable
  (testing "a stack either fits or it doesn't"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (stackup 0.35) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :stack-exceeds-allowable (:rule %)) (:violations v))))))

(deftest hard-on-unknown-process
  (let [st (fresh-store)
        v (governor/check req {} (assoc (measure 10.0) :process-id "P-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-process (:rule %)) (:violations v)))))

(deftest hard-on-foreign-process
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (measure 10.0) st)]
      (is (:hard? v))
      (is (some #(= :process-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (measure 10.0) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (measure 10.0) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-deviation-approval
  (let [st (fresh-store)
        v (governor/check req {} {:op :approve-deviation :effect :propose
                                  :process-id "P-1" :confidence 0.9 :stake :high} st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (measure 10.0) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
