(ns app
  (:require ["better-sqlite3$default" :as sql]
            ["fs" :as fs]
            ["path" :as path]
            ["inquirer$default" :as inquirer]
            ["moment$default" :as moment]
            ["console" :as console]
            ["figlet$default" :as figlet]
            [nbb.core]
            [promesa.core :as p]
            [clojure.string :as s]))

;;; General Functions
(defn script-loc
  "Function to get the Script Location"
  []
  (let [script (path/resolve nbb.core/*file*)]
    (-> script
        (s/split #"/")
        drop-last
        (->>
         (interleave (repeat "/")))
        rest
        rest
        s/join)))

(defn get-banner
  [banner-message]
  (figlet/textSync banner-message))

(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))

(defn next-month
  [curr-month]
  (.format
   (.add
    (.month
     (moment)
     curr-month)
    1
    "months")
   "MMMM"))

;;; Defs
;;; Defining cmd-line args for use via index.mjs
(def cmd-line-args (not-empty (js->clj (.slice js/process.argv 2))))

(def db (sql. (str (script-loc) "/db/birthdays.db")))

(def valid-params '("list" "list-people" "update" "delete" "search-day" "search-month" "reminder" "help"))

(def top-div
  "***************************************************************************************************************")

(def hlp-msg (str (fs/readFileSync (str
                                    (script-loc)
                                    "/help.txt"))))

(def inv-msg (str (fs/readFileSync (str
                                    (script-loc)
                                    "/invalid.txt"))))

(def questions (clj->js [{:name "name"
                          :type "input"
                          :message "Whose Birthday do you want to store?"}
                         {:name "day"
                          :type "number"
                          :message "What Day is their Birthday?"
                          :validate (fn [v]
                                      (<= 1 v 31))}
                         {:name "month"
                          :type "list"
                          :message "What Month is their Birthday"
                          :choices (moment/months)}
                         {:name "year"
                          :type "number"
                          :message "What Year were they born?"
                          :validate (fn [v]
                                      (<= 1900 v (.format (moment) "YYYY")))}
                         {:name "gift-idea"
                          :type "input"
                          :message "What shall you get for them?"
                          :default "Beats the shit outta me!"}]))

;;; Action Functions
(defn write-birthday
  "Function to persist a Birthday Record"
  [name day month year gift-idea]
  (p/let [p_query (.prepare db "INSERT INTO people
                                (name, day, month, year, fname, sname)
                                VALUES
                                (?,?,?,?,?,?)")
          fname (first (s/split name #" "))
          sname (last (s/split name #" "))
          p_resp (.run p_query name day month year fname sname)
          p_id (.-lastInsertRowid p_resp)
          g_query (.prepare db "INSERT INTO gift_ideas(personID, gift_idea) VALUES (?,?)")
          g_resp (.run g_query p_id gift-idea)
          res (if (= 1 (.-changes g_resp)) "Success!" "Something went wrong...")]
    res))

(defn create-birthday-entry
  "Function to store Birthday Entry in DB"
  [banner] 
  (println top-div)
  (println banner)
  (println top-div)
  (p/let [_answers (inquirer/prompt questions)
          answers (js->clj _answers :keywordize-keys true)
          {:keys [name day month year gift-idea]} answers]
    (write-birthday name day month year gift-idea)))

(defn make-birthday-message
  "Function to build a birthday message"
  [type name gift-idea]
  (cond
    (= type "month") (str "It's "
                          name 
                          "'s"
                          " Birthday this month and they want a "
                          gift-idea
                          " 🏆")
    :else
    (str "It's "
         name 
         "'s"
         " Birthday today and they want a "
         gift-idea
         " 🏆")))

(defn list-birthdays
  "Function to retrieve birthdays"
  [banner]
  (println top-div)
  (println banner)
  (println top-div)
  (p/let [month (.format (moment) "MMMM")
          day (.date (moment))
          b_query (.prepare
                   db
                   "SELECT * 
                    FROM people 
                    AS 
                    p, gift_ideas AS g 
                    WHERE 
                    day=? 
                    AND month=? 
                    AND p.personID=g.personID")
          b_resp (.all b_query day month)
          b_res (js->clj b_resp :keywordize-keys true)
          b_mesgs (reduce
                   (fn [acc coll]
                     (conj
                      acc
                      {:birthday-message
                       (make-birthday-message
                        "day"
                        (:name coll)
                        (:gift_idea coll))}))
                   []
                   b_res)]
         (if (= 0 (count b_mesgs))
           (println "No Birthdays Today")
           (console/table (clj->js b_mesgs)))))

(defn get-people
  "Function to retrieve the People in the Database"
  []
  (let [u_query (.prepare db "SELECT * FROM people ORDER BY sname,fname ASC")
          u_resp (.all u_query)
          u_res (js->clj u_resp :keywordize-keys true)]
    u_res))

(defn list-people
  "Function to output all of the people in the DB on the command line"
  [banner]
  (println top-div)
  (println banner)
  (println top-div)
  (let [users (get-people)]
    (console/table (clj->js users))))

(defn get-people-choices
  "Function to get realised choices from get-people"
  []
  (let [people (get-people)]
    (reduce
     (fn
       [acc coll]
       (conj
        acc
        {:name (:name coll)
         :value (:personID coll)}))
     []
     people)))

(def update-questions 
  (let [choices (get-people-choices)]
         (clj->js [{:name "personID"
                    :type "list"
                    :message "Whose Birthday Gift do you want to update?"
                    :choices choices}
                   {:name "gift-idea"
                    :type "input"
                    :message "What would you like to get for them as a gift?"}])))

(defn update-birthday-gift
  "Function to write update to Birthday Gift Idea to Database"
  [personID gift-idea]
  (let [up_query (.prepare db "UPDATE gift_ideas SET gift_idea=? WHERE personID=?")
          up_resp (.run up_query gift-idea personID)
          up_id (.-changes up_resp)
          res (if (= 1 up_id) "Success!" "Something went wrong...")]
    (println res)))

(defn update-birthday-entry
  "Function to update Birthday Gift Entry in DB"
  [banner]
  (println top-div)
  (println banner)
  (println top-div)
  (p/let [_answers (inquirer/prompt update-questions)
          answers (js->clj _answers :keywordize-keys true)
          {:keys [personID gift-idea]} answers]
    (update-birthday-gift personID gift-idea)))

(def delete-questions
  (let [choices (get-people-choices)]
    (clj->js [{:name "personID"
               :type "list"
               :message "Who do you want to remove?"
               :choices choices}])))

(defn delete-birthday
  "Function to delete someone from the birthdays DB"
  [personID]
  (p/let [p_query (.prepare db "DELETE FROM people WHERE personID=?")
          _ (.run p_query personID) 
          g_query (.prepare db "DELETE FROM gift_ideas WHERE personID=?")
          g_resp (.run g_query personID)
          res (if (= 1 (.-changes g_resp)) "Success!" "Something went wrong...")]
    res))

(defn delete-birthday-entry
  "Function to delete Birthday Entries all together (manage duplicates,
   un-friend people etc.)"
  [banner]
  (println top-div)
  (println banner)
  (println top-div)
  (p/let [_answers (inquirer/prompt delete-questions)
          answers (js->clj _answers :keywordize-keys true)
          {:keys [:personID]} answers]
         (delete-birthday personID)))

(defn search-birthdays-by-day
  "Function to search for Birthdays on a specific day"
  [banner day month]
  (println top-div)
  (println banner)
  (println top-div)
  (let [s_query (.prepare db "SELECT * FROM 
                              people AS p, gift_ideas AS g 
                              WHERE day=? 
                              AND 
                              month=? 
                              AND p.personID=g.personID 
                              ORDER BY sname ASC")
        s_resp (.all s_query day month)
        s_res (js->clj s_resp :keywordize-keys true)
        s_mesgs (reduce
                 (fn [acc coll]
                   (conj
                    acc
                    {:day (:day coll)
                     :month month
                     :birthday-message
                     (make-birthday-message
                      "day"
                      (:name coll)
                      (:gift_idea coll))}))
                 []
                 s_res)]
    (if (= 0 (count s_mesgs))
      (println "No Birthdays on" (str day " " month))
      (console/table (clj->js s_mesgs)))))

(defn search-birthdays-by-month
  "Function to search for Birthdays in a specific month"
  [month]
  (let [s_query (.prepare db "SELECT * FROM 
                              people AS p, gift_ideas AS g 
                              WHERE month=? 
                              AND p.personID=g.personID 
                              ORDER BY day,sname ASC")
        s_resp (.all s_query month)
        s_res (js->clj s_resp :keywordize-keys true)]
    (reduce
     (fn [acc coll]
       (conj
        acc
        {:day (:day coll)
         :month month
         :birthday-message
         (make-birthday-message
          "month"
          (:name coll)
          (:gift_idea coll))}))
     []
     s_res)))

(defn show-search-results-month
  [banner month]
  (println top-div)
  (println banner)
  (println top-div)
  (let [s_mesgs (search-birthdays-by-month month)]
    (if (= 0 (count s_mesgs))
      (println "No Birthdays in" (str month))
      (console/table (clj->js s_mesgs)))))

(defn show-reminder
  [banner month]
  (println top-div)
  (println banner)
  (println top-div)
  (let [bs-this-month (search-birthdays-by-month month)
        bs-next-month (search-birthdays-by-month (next-month month))]
    (if (= 0 (count bs-this-month))
      (println "No Birthdays in" (str month))
      (console/table (clj->js bs-this-month)))
    (if (= 0 (count bs-next-month))
      (println "No Birthdays in" (str (next-month month)))
      (console/table (clj->js bs-next-month)))))

(defn help-message
  "Function to display help and other on-invocation messages"
  [banner]
  (println top-div)
  (println banner)
  (println hlp-msg))

(defn invalid-message
  "Function to display invalid param message"
  [banner]
  (println top-div)
  (println banner)
  (println inv-msg))

;;; Despatcher
(defn despatch-action
  "function to despatch actions passing in banner and other needed params"
  [mode args]
  (p/let [banner-txt (get-banner "BIRTHDAYMAN - SQLITE")]
    (cond
      (= mode "create") (create-birthday-entry banner-txt)
      (= mode "list") (list-birthdays banner-txt)
      (= mode "list-people") (list-people banner-txt)
      (= mode "update") (update-birthday-entry banner-txt)
      (= mode "delete") (delete-birthday-entry banner-txt)
      (= mode "search-day") (search-birthdays-by-day
                             banner-txt
                             (first args)
                             (second args))
      (= mode "search-month") (show-search-results-month
                               banner-txt
                               (first args))
      (= mode "reminder") (show-reminder
                           banner-txt
                           (first args))
      (= mode "help") (help-message banner-txt)
      (= mode "invalid") (invalid-message banner-txt))))

;;; Triggering code that is eval'ed and run
(let [mode (first cmd-line-args)]
  (if (nil? mode)
    (despatch-action "create" '())
    (if (not (in? valid-params mode))
      (despatch-action "invalid" '())
      (despatch-action mode (rest cmd-line-args)))))