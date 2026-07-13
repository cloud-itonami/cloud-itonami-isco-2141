(ns indprod.advisor
  "IndustrialProductionEngineersAdvisor — proposes a manufacturing QA
  operation (approve a measurement, approve a tolerance stack-up,
  approve a deviation) for a registered organization. Swappable
  mock/llm; the advisor ONLY proposes — `indprod.governor` checks
  interval containment and stack-up arithmetic independently. Modeled
  on cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-measurement|:approve-stackup|:approve-deviation
               :effect :propose :process-id str :measured number
               :stack number :stake kw :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake process-id measured stack] :as request}]
  {:op op
   :effect :propose
   :process-id process-id
   :measured measured
   :stack stack
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are an industrial/production engineering advisor. Given a
   request, propose an :op, the :process-id, :measured value and/or
   :stack sum, an honest :confidence and a :stake. Never call an
   out-of-band measurement or an over-stack assembly conforming — the
   governor checks interval containment and the stack-up arithmetic.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
