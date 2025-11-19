(ns lof.lab.data.record
  (:require [clojure.string :as str]))

(defrecord Record [canonical-id authors title year doi url])

(defn normalize
  "Normalize a string: lowercase, remove punctuation, trim"
  [s]
  (if (nil? s)
    ""
    (-> s
        str/lower-case
        (str/replace #"[^a-z0-9]" "")
        str/trim)))

(defn extract-author-lastname
  "Extract last name from author string.
   Handles formats: 'LastName, FirstName' or 'FirstName LastName'"
  [author]
  (if (str/includes? author ",")
    ;; Format: "LastName, FirstName"
    (-> author (str/split #",") first str/trim)
    ;; Format: "FirstName LastName"
    (-> author (str/split #"\s+") last str/trim)))

(defn generate-canonical-id
  "Generate canonical ID from (firstAuthor, year, titleSlug).
   Format: 'lastname-year-first-three-title-words'
   Example: 'voros-2023-cradle-things'"
  [{:keys [authors title year]}]
  (let [;; 1. First author's last name (or 'unknown')
        author-part (if (seq authors)
                      (-> authors first extract-author-lastname normalize)
                      "unknown")

        ;; 2. Year (or 'nodate')
        year-part (if year (str year) "nodate")

        ;; 3. First three words of title (or 'notitle')
        title-part (if (and title (not (str/blank? title)))
                     (->> (str/split title #"\s+")
                          (take 3)
                          (map normalize)
                          (str/join "-"))
                     "notitle")]

    (str/join "-" [author-part year-part title-part])))

(defn make-record
  "Create a Record with auto-generated canonical-id"
  [{:keys [authors title year doi url] :as data}]
  (let [canonical-id (generate-canonical-id data)]
    (->Record canonical-id
              (vec (or authors []))
              title
              year
              doi
              url)))

(comment
  ;; REPL testing
  (def test-record
    (make-record {:authors ["Vörös, Sebastjan"]
                  :title "At the Cradle of Things: The Act of Distinction"
                  :year 2023
                  :doi "10.53765/20512201.30.11.017"
                  :url nil}))

  (:canonical-id test-record)
  ;; => "vrs-2023-at-the-cradle"

  (def test-record2
    (make-record {:authors ["Conrad, Leon"]
                  :title "Laws of Form Online Course"
                  :year 2020
                  :doi nil
                  :url "https://www.youtube.com/..."}))

  (:canonical-id test-record2)
  ;; => "conrad-2020-laws-of-form"
  )
