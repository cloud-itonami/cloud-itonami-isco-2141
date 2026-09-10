(ns indprod.governor
  "IndustrialProductionEngineersGovernor — the independent safety/
  traceability layer for the ISCO-08 2141 community industrial &
  production engineers actor (itonami actor pattern, ADR-2607011000 /
  CLAUDE.md Actors section). Modeled on cloud-itonami-isco-4311's
  bookkeeping.governor. Manufacturing twist: a measurement's
  conformance is INTERVAL CONTAINMENT against the registered
  tolerance band [LSL, USL], and a proposed tolerance stack-up is
  ARITHMETIC SUM against the registered allowable stack — a part
  either falls in the band or it doesn't, and a stack either fits or
  it doesn't; neither is a judgement call.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance — the organization must be registered.
    2. no-actuation      — proposal :effect must be :propose.
    3. process basis     — a measurement/stack-up must cite a
                           REGISTERED process belonging to this
                           client.
    4. tolerance containment — a proposed measurement must satisfy
                           LSL <= measured <= USL (interval
                           containment against the registered band).
    5. stack-up limit     — a proposed cumulative tolerance stack must
                           not exceed the process's registered
                           :allowable-stack (arithmetic sum vs a
                           registered ceiling).
  ESCALATION invariants (:escalate? true, human sign-off):
    6. :op :approve-deviation (accepting an out-of-spec part or an
                           over-stack assembly anyway).
    7. low confidence (< `confidence-floor`)."
  (:require [indprod.store :as store]))

(def confidence-floor 0.6)

(defn- hard-violations [{:keys [request proposal]} client-record p]
  (let [{:keys [op measured stack]} proposal
        measure? (= :approve-measurement op)
        stackup? (= :approve-stackup op)
        process-op? (or measure? stackup?)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (and process-op? (nil? p))
      (conj {:rule :unknown-process :detail "未登録 process の測定/積み上げは受理不可"})

      (and process-op? p (not= (:client-id p) (:client-id request)))
      (conj {:rule :process-wrong-client :detail "process が別 client のもの"})

      (and measure? p (number? measured)
           (or (< measured (:lsl p)) (> measured (:usl p))))
      (conj {:rule :out-of-tolerance
             :detail (str "測定値 " measured " が許容帯 [" (:lsl p) ", " (:usl p)
                          "] の外（区間包含は算術であって判断ではない）")})

      (and stackup? p (number? stack) (> stack (:allowable-stack p)))
      (conj {:rule :stack-exceeds-allowable
             :detail (str "累積公差 " stack " > 許容スタック "
                          (:allowable-stack p) "（積み上げ算術は超過か否かの二値）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `indprod.store/Store`. Pure — never mutates the
  store."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        p (some->> (:process-id proposal) (store/process store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record p)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (= :approve-deviation (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
