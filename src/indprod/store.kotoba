(ns indprod.store
  "SSoT for the ISCO-08 2141 community industrial & production
  engineers actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md
  Actors section). Modeled on cloud-itonami-isco-4311's
  bookkeeping.store.

  Domain:

    client    — a registered organization (:client-id, :name)
    process   — a registered manufacturing process step {:process-id
                :client-id :name :lsl number :usl number
                :allowable-stack number}. `:lsl`/`:usl` are the
                registered tolerance band (lower/upper spec limit) for
                a single dimension; `:allowable-stack` is the
                registered maximum cumulative tolerance for an
                assembly of steps citing this process.
    record    — a committed operating record (approved measurement,
                approved stack-up) — written ONLY via commit-record!.
    ledger    — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (process [s process-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-process! [s p])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (process [_ process-id] (get-in @a [:processes process-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-process! [s p]
    (swap! a assoc-in [:processes (:process-id p)] p) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :processes {} :records [] :ledger []}
                                   seed)))))
