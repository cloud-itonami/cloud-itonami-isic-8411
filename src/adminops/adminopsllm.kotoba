(ns adminops.adminopsllm
  "AdminOps-LLM client -- the *contained intelligence node* for the
  community-public-administration actor.

  It normalizes case intake, drafts a per-jurisdiction administrative-
  procedure/appeal-rights evidence checklist, drafts the case-decision
  action, and drafts the citizen-notification action. CRITICAL: it is
  a smart-but-untrusted advisor. It returns a *proposal* (with a
  rationale + the fields it cited), never a committed record or a
  real decision/notification. Every output is censored downstream by
  `adminops.governor` before anything touches the SSoT, and `:case/
  decide`/`:case/notify` proposals NEVER auto-commit at any phase --
  see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/decide-case | :actuation/notify-citizen | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [kotoba.lang.text :as str]
            [adminops.facts :as facts]
            [adminops.registry :as registry]
            [adminops.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the applicant, amount/rate or jurisdiction. High
  confidence, low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "案件記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :case/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- assess-jurisdiction
  "Per-jurisdiction administrative-procedure/appeal-rights evidence
  checklist draft. `:no-spec?` injects the failure mode we must
  defend against: proposing a checklist for a jurisdiction with NO
  official spec-basis in `adminops.facts` -- the Public Administration
  Governor must reject this (never invent a jurisdiction's
  requirements)."
  [db {:keys [subject no-spec?]}]
  (let [c (store/case-record db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction c))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "adminops.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :assessment/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :assessment/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- propose-decision
  "Draft the actual CASE-DECISION action -- deciding a real case.
  ALWAYS `:stake :actuation/decide-case` -- this is a REAL-WORLD act
  (an authorized official issues a real official decision), never a
  draft the actor may auto-run. See README `Actuation`: no phase ever
  adds this op to a phase's `:auto` set (`adminops.phase`); the
  governor also always escalates on `:actuation/decide-case`. Two
  independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [c (store/case-record db subject)]
    {:summary    (str subject " 向け決定提案"
                      (when c (str " (applicant=" (:applicant c) ")")))
     :rationale  (if c
                   (str "within-delegated-authority?=" (:within-delegated-authority? c)
                        " jurisdiction=" (:jurisdiction c))
                   "caseが見つかりません")
     :cites      (if c [subject] [])
     :effect     :case/mark-decided
     :value      {:case-id subject}
     :stake      :actuation/decide-case
     :confidence (if (and c (:within-delegated-authority? c)) 0.9 0.3)}))

(defn- propose-notification
  "Draft the actual CITIZEN-NOTIFICATION action -- notifying a real
  citizen of a real decision (triggering assessed-fee accrual and,
  when adverse, an appeal window). ALWAYS `:stake :actuation/notify-
  citizen` -- this is a REAL-WORLD act (a real legal notice is issued
  to a real citizen), never a draft the actor may auto-run. See
  README `Actuation`: no phase ever adds this op to a phase's `:auto`
  set (`adminops.phase`); the governor also always escalates on
  `:actuation/notify-citizen`. Two independent layers agree,
  deliberately."
  [db {:keys [subject]}]
  (let [c (store/case-record db subject)
        fee-ok? (and c (registry/assessed-fee-matches-claim? c))
        appeal-ok? (and c (or (not (:decision-adverse? c)) (:appeal-rights-disclosed? c)))]
    {:summary    (str subject " 向け通知提案"
                      (when c (str " (applicant=" (:applicant c) ")")))
     :rationale  (if c
                   (str "claimed-fee=" (:claimed-fee c)
                        " independent-recompute=" (registry/compute-assessed-fee c)
                        " appeal-ok?=" appeal-ok?)
                   "caseが見つかりません")
     :cites      (if c [subject] [])
     :effect     :case/mark-notified
     :value      {:case-id subject}
     :stake      :actuation/notify-citizen
     :confidence (if (and fee-ok? appeal-ok?) 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :case/intake            (normalize-intake db request)
    :jurisdiction/assess         (assess-jurisdiction db request)
    :case/decide                     (propose-decision db request)
    :case/notify                          (propose-notification db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたは地域公共行政サービスの決定・通知エージェントの助言者です。"
       "与えられた事実のみに基づき、提案を1つだけEDNマップで返します。"
       "説明や前置きは一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:case/upsert|:assessment/set|:case/mark-decided|"
       ":case/mark-notified) "
       ":stake(:actuation/decide-case か :actuation/notify-citizen か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"
       "決定権限の範囲や審査請求教示の実施状況を偽って報告してはいけません。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :jurisdiction/assess    {:case (store/case-record st subject)}
    :case/decide            {:case (store/case-record st subject)}
    :case/notify            {:case (store/case-record st subject)}
    {:case (store/case-record st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Public Administration
  Governor escalates/holds -- an LLM hiccup can never auto-decide a
  case or auto-notify a citizen."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :adminopsllm-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
