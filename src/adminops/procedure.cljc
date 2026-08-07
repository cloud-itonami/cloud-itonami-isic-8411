(ns adminops.procedure
  "**法定手続きの期限**への橋渡し —— `kotoba-lang/tetsuzuki`。

  この actor は「案件を受け付け、決定し、通知する」までを持っていたが、
  **その決定が法定期限との関係でまだ可能なのか**は誰も見ていなかった。
  行政庁を縛る期限は、徒過すると法域ごとにまったく違う効果を生む:

  | | 徒過したとき |
  |---|---|
  | DEU VwVfG §42a | **許可されたものとみなす**（Genehmigungsfiktion） |
  | CAN ATIA s.10(3) | **拒否したものとみなす** |
  | USA FOIA §552(a)(6)(C) | 請求者は出訴適格を得る |
  | JPN 行政手続法 §6 | 標準処理期間は努力目標。効果なし |

  最初の 2 つが決定的である。**みなし処分が既に効力を生じている案件を、
  行政庁が今から『決定』することはできない** —— 法律上の擬制が先に効いて
  いるからで、そこへ別の決定を重ねれば、擬制と矛盾する処分を作ることになる。

  ## この ns が持たないもの

  期限・徒過効果の中身は持たない（`tetsuzuki.catalog` の仕事）。ここは
  case 自身の記録から**独立に再計算**し、決定してよいかの語彙を返すだけ。
  **提案が主張した期限や徒過状態は読まない。**

  ## 手続き id を持たない案件

  `:procedure-id` を持たない案件はこの検査を素通りする。この actor が扱う
  コミュニティ行政の案件すべてが `tetsuzuki` の一般カタログに載っている
  わけではないためで、**載っていないことを違反にしない**。ただし
  `:procedure-id` を**宣言している**案件は必ず検査される —— 宣言した以上、
  その手続きの法定期限に従う。"
  (:require [tetsuzuki.catalog :as catalog]
            [tetsuzuki.deadline :as deadline]
            [tetsuzuki.review :as review]))

(def precluding-lapse-effects
  "既に効力を生じていて、**行政庁が今から決定することを妨げる**徒過効果。

  `:exhausts-remedy`（出訴適格）と `:none`（効果なし）は妨げない ——
  前者は請求者に選択肢を与えるだけ、後者は何も起きていない。"
  #{:deemed-granted :deemed-refused})

(defn declared?
  "その案件が法定手続きを宣言しているか。"
  [c]
  (some? (:procedure-id c)))

(defn basis
  "宣言された手続きの entry。宣言していない、または表に無ければ nil。"
  [{:keys [jurisdiction procedure-id]}]
  (when procedure-id
    (catalog/procedure jurisdiction procedure-id)))

(defn deadline-status
  "case 自身の記録から**独立に**期限の状態を出す。

  `ctx` は `{:anchors {kw epoch-day} :calendar {...}}`。
  観測日は case の `:observed-epoch-day`（今どこにいるか）。"
  [c ctx]
  (if-let [p (basis c)]
    (deadline/status (:proc/deadline p) ctx (:observed-epoch-day c))
    (if (declared? c) :no-basis :not-declared)))

(defn lapse-effect
  "徒過していれば、その効果。徒過していなければ nil。"
  [c ctx]
  (when-let [p (basis c)]
    (deadline/lapse-consequence p ctx (:observed-epoch-day c))))

(defn decision-precluded?
  "みなし処分が既に効力を生じているために、行政庁が今から決定できない状態か。"
  [c ctx]
  (boolean (precluding-lapse-effects (lapse-effect c ctx))))

(def blocking-deadline-statuses
  "決定を止めるべき期限状態。**未解決の理由はすべて止める** ——
  期限を確認できないまま処分をしない。`:not-declared`（手続きを宣言して
  いない案件）と `:within` は止めない。"
  #{:no-basis :unknown-anchor :needs-business-calendar :needs-calendar-date
    :unknown-observation :authority-published :unknown-unit})

(defn deadline-blocking? [status]
  (boolean (blocking-deadline-statuses status)))

(defn formal-review-outcome
  "宣言された手続きの形式審査。宣言していなければ `:not-declared`。"
  [c]
  (if-let [p (basis c)]
    (review/outcome p (:review-items-satisfied c))
    (if (declared? c) :unknown :not-declared)))

(defn explain
  "HOLD 理由の人間向け説明。文言は tetsuzuki 側の語彙をそのまま運ぶ
  （governor が説明を作り直すと、法の説明が 2 か所に分かれる）。"
  [c ctx]
  (let [st (deadline-status c ctx)
        eff (lapse-effect c ctx)]
    (str "手続き " (:jurisdiction c) "/" (:procedure-id c)
         " の期限状態=" st
         (when eff (str " / 徒過効果=" eff))
         (when-let [p (basis c)]
           (str " / " (deadline/describe p))))))
