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

;;; Defining cmd-line args for use via index.mjs
(def cmd-line-args (not-empty (js->clj (.slice js/process.argv 2))))

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

(def db (sql. (str (script-loc) "/db/birthdays.db")))

(defn get-banner
  [banner-message]
  (figlet/textSync banner-message))

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

(defn build-create-birthday-entry
  []
  (p/let [banner-text (get-banner "BIRTHDAYMAN - SQLITE")]
    (create-birthday-entry banner-text)))

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

(defn build-list-birthdays
  []
  (p/let [banner-text (get-banner "BIRTHDAYMAN - SQLITE")]
    (list-birthdays banner-text)))

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

(defn build-list-people
  []
  (p/let [banner-text (get-banner "BIRTHDAYMAN - SQLITE")]
    (list-people banner-text)))

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

(defn build-update-birthday-entry
  []
  (p/let [banner-text (get-banner "BIRTHDAYMAN - SQLITE")]
    (update-birthday-entry banner-text)))

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

(defn build-delete-birthday-entry
  []
  (p/let [banner-text (get-banner "BIRTHDAYMNA - SQLITE")]
    (delete-birthday-entry banner-text)))

(defn search-birthdays-by-day
  "Function to search for Birthdays on a specific day"
  [day month banner]
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

(defn build-search-birthdays-by-day
  [day month]
  (p/let [banner-text (get-banner "BIRTHDAYMAN - SQLITE")]
    (search-birthdays-by-day day month banner-text)))

(defn search-birthdays-by-month
  "Function to search for Birthdays in a specific month"
  [month banner]
  (println top-div)
  (println banner)
  (println top-div)
  (let [s_query (.prepare db "SELECT * FROM 
                              people AS p, gift_ideas AS g 
                              WHERE month=? 
                              AND p.personID=g.personID 
                              ORDER BY day,sname ASC")
        s_resp (.all s_query month)
        s_res (js->clj s_resp :keywordize-keys true)
        s_mesgs (reduce
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
                 s_res)]
    (if (= 0 (count s_mesgs))
      (println "No Birthdays in" (str month))
      (console/table (clj->js s_mesgs)))))

(defn build-search-birthdays-by-month
  [month]
  (p/let [banner-text (get-banner "BIRTHDAYMAN - SQLITE")]
    (search-birthdays-by-month month banner-text)))

(defn help-message
  "Function to display help and other on-invocation messages"
  [mode banner]
  (println top-div)
  (println banner)
  (cond
    (= mode "help") (println hlp-msg)
    (= mode "invalid") (println inv-msg)
    :else (println "Unknown Help Mode")))

(defn build-help-message
  [mode]
  (p/let [banner-txt (get-banner "BIRTHDAYMAN - SQLITE")]
    (help-message mode banner-txt)))

(cond
  (= (first cmd-line-args) "list") (build-list-birthdays)
  (= (first cmd-line-args) "list-people") (build-list-people)
  (= (first cmd-line-args) "update") (build-update-birthday-entry)
  (= (first cmd-line-args) "delete") (build-delete-birthday-entry)
  (= (first cmd-line-args) "search-day") (build-search-birthdays-by-day
                                                (second cmd-line-args)
                                                (last cmd-line-args))
  (= (first cmd-line-args) "search-month") (build-search-birthdays-by-month
                                                  (last cmd-line-args))
  (= (first cmd-line-args) "help") (build-help-message "help")
  :else (if (= 0 (count cmd-line-args))
          (build-create-birthday-entry)
          (build-help-message "invalid")))