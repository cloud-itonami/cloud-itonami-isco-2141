(ns indprod.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [indprod.actor :as actor]
            [indprod.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Trade"})
    (store/register-process! st {:process-id "P-1" :client-id "client-1"
                                 :name "shaft-turning" :lsl 9.95 :usl 10.05
                                 :allowable-stack 0.30})
    st))

(deftest commits-an-in-tolerance-measurement
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-measurement :stake :low
                 :process-id "P-1" :measured 10.0}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-out-of-tolerance-measurement
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-measurement :stake :low
                 :process-id "P-1" :measured 10.5}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-accepts-deviation-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-deviation :stake :high
                 :process-id "P-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
