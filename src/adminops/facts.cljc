(ns adminops.facts
  "Per-jurisdiction administrative-procedure AND appeal-rights
  regulatory catalog -- the G2-style spec-basis table the Public
  Administration Governor checks every `:jurisdiction/assess`
  proposal against ('did the advisor cite an OFFICIAL public source
  for this jurisdiction's requirements, or did it invent one?').

  This blueprint's own text (docs/business-model.md's own Trust
  Controls: 'decisions outside authority are blocked; notifications
  are auditable; appeals are mandatory') names two real, distinct
  regulatory concerns: the general administrative-procedure/ultra-
  vires framework a decision must fall within (independent of whether
  the decision is adverse), and a SEPARATE appeal-rights/notice-of-
  remedies regime specifically requiring an adverse decision's
  notification to disclose the citizen's appeal rights (independent
  of whether the deciding official had proper authority -- a properly
  authorized decision can still be notified without appeal-rights
  disclosure, and a decision with full appeal-rights disclosure can
  still have been made outside the deciding official's own delegated
  authority). Each jurisdiction entry below therefore cites BOTH the
  general administrative-procedure/ultra-vires law AND a SEPARATE
  appeal-rights/notice law.

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries. Like
  `employmentops`/7810's own work-authorization sub-citation, ALL FOUR
  seeded jurisdictions actually have a real appeal-rights sub-citation
  here, reported honestly (a full-coverage sub-citation, matching
  `quarryops`/0810's own blast-safety and `agronomyops`/0162's own
  water-buffer full coverage rather than `hospitalityops`/5510's own
  honest single-jurisdiction gap).")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  intake/registration/decision-record evidence set (PLUS an appeal-
  rights-disclosure record for every seeded jurisdiction);
  `:legal-basis` / `:owner-authority` / `:provenance` are the G2
  citation the governor requires before any `:jurisdiction/assess`
  proposal can commit. `:appeal-owner-authority` / `:appeal-legal-
  basis` / `:appeal-provenance` are the SEPARATE appeal-rights
  citation the governor's `appeal-rights-notice-missing?` check is
  grounded in."
  {"JPN" {:name "Japan"
          :owner-authority "総務省 (Ministry of Internal Affairs and Communications, MIC) / 地方公共団体"
          :legal-basis "行政手続法 (Administrative Procedure Act) 及び 地方自治法 (Local Autonomy Act)"
          :national-spec "行政庁の権限に関する委任規則及び処分基準"
          :provenance "https://www.soumu.go.jp/main_sosiki/gyoukan/kanri/gyoseitetuduki.html"
          :required-evidence ["受付記録 (intake record)"
                              "登録記録 (registration record)"
                              "決定記録 (decision record)"
                              "審査請求教示記録 (appeal-rights-disclosure record)"]
          :appeal-owner-authority "総務省 / 行政不服審査会"
          :appeal-legal-basis "行政不服審査法 (Administrative Appeal Act) 第82条 (教示)"
          :appeal-provenance "https://www.soumu.go.jp/main_sosiki/gyoukan/kanri/fufuku.html"}
   "USA" {:name "United States"
          :owner-authority "Administrative Conference of the United States (ACUS)"
          :legal-basis "Administrative Procedure Act (APA), 5 U.S.C. §706"
          :national-spec "APA scope-of-review provisions (agency action in excess of statutory authority)"
          :provenance "https://www.acus.gov/research-projects/administrative-procedure-act"
          :required-evidence ["Intake record"
                              "Registration record"
                              "Decision record"
                              "Appeal-rights-disclosure record"]
          :appeal-owner-authority "Administrative Conference of the United States (ACUS)"
          :appeal-legal-basis "APA §555(e) (prompt notice of denial with a brief statement of grounds)"
          :appeal-provenance "https://www.law.cornell.edu/uscode/text/5/555"}
   "GBR" {:name "United Kingdom"
          :owner-authority "Cabinet Office / Parliamentary and Health Service Ombudsman"
          :legal-basis "Judicial review principles (ultra vires doctrine, Anisminic v Foreign Compensation Commission)"
          :national-spec "Cabinet Office Judge Over Your Shoulder guidance"
          :provenance "https://www.gov.uk/government/publications/judge-over-your-shoulder"
          :required-evidence ["Intake record"
                              "Registration record"
                              "Decision record"
                              "Appeal-rights-disclosure record"]
          :appeal-owner-authority "Tribunals judiciary / Ministry of Justice"
          :appeal-legal-basis "Tribunals, Courts and Enforcement Act 2007 (notice of appeal rights)"
          :appeal-provenance "https://www.gov.uk/courts-tribunals/first-tier-tribunal-general-regulatory-chamber"}
   "DEU" {:name "Germany"
          :owner-authority "Bundesministerium des Innern (Federal Ministry of the Interior) / Kommunalaufsicht"
          :legal-basis "Verwaltungsverfahrensgesetz (VwVfG) §44 (Nichtigkeit eines Verwaltungsaktes)"
          :national-spec "VwVfG Zuständigkeits- und Ermächtigungsvorschriften"
          :provenance "https://www.gesetze-im-internet.de/vwvfg/__44.html"
          :required-evidence ["Antragsprotokoll (intake record)"
                              "Registrierungsprotokoll (registration record)"
                              "Bescheidprotokoll (decision record)"
                              "Rechtsbehelfsbelehrungsnachweis (appeal-rights-disclosure record)"]
          :appeal-owner-authority "Verwaltungsgerichte (administrative courts)"
          :appeal-legal-basis "VwVfG §37 Abs. 6 (Rechtsbehelfsbelehrung)"
          :appeal-provenance "https://www.gesetze-im-internet.de/vwvfg/__37.html"}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to decide a case
  or notify a citizen on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-8411 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `adminops.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))

(defn appeal-spec-basis
  "The jurisdiction's appeal-rights requirement map, or nil -- nil
  means this jurisdiction has NO formal statutory appeal-rights-notice
  regime this catalog is aware of. In this R0 catalog all four seeded
  jurisdictions actually have one, reported honestly (a full-coverage
  sub-citation, matching `quarryops`/0810's own blast-safety and
  `agronomyops`/0162's own water-buffer full coverage)."
  [iso3]
  (when-let [sb (spec-basis iso3)]
    (when (:appeal-owner-authority sb)
      (select-keys sb [:appeal-owner-authority :appeal-legal-basis :appeal-provenance]))))
