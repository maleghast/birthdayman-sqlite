(ns app
  (:require ["better-sqlite3$default" :as sql]
            ["fs" :as fs]
            ["inquirer$default" :as inquirer]
            ["moment$default" :as moment]
            [promesa.core :as p]
            [clojure.string :as s]))

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

(def db (sql. "birthdays.db"))

(defn write-birthday
  "Function to persist a Birthday Record"
  [name day month year gift-idea]
  (p/let [p_query (.prepare db "INSERT INTO people(name, day, month, year, fname, sname) VALUES (?,?,?,?,?,?)")
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
  []
  (p/let [_answers (inquirer/prompt questions)
          answers (js->clj _answers :keywordize-keys true)
          {:keys [name day month year gift-idea]} answers]
    (write-birthday name day month year gift-idea)))

(defn list-birthdays
  "Function to retrieve birthdays"
  []
  (p/let [month (.format (moment) "MMMM")
          day (.date (moment))
          b_query (.prepare
                   db
                   "SELECT * FROM people AS p, gift_ideas AS g WHERE day=? AND month=? AND p.personID=g.personID")
          b_resp (.all b_query day month)
          b_res (js->clj b_resp :keywordize-keys true)]
    (run! (fn [{:keys [name gift_idea]}]
            (println "It's" (str name "'s") "Birthday today and they want a" (str gift_idea) "🏆")) b_res)))

(defn get-people
  "Function to retrieve the People in the Database"
  []
  (let [u_query (.prepare db "SELECT * FROM people ORDER BY sname,fname ASC")
          u_resp (.all u_query)
          u_res (js->clj u_resp :keywordize-keys true)]
    u_res))

(defn list-people
  "Function to output all of the people in the DB on the command line"
  []
  (let [users (get-people)]
    (doseq [user users]
      (println (:personID user) "-"(:name user) "-" (:day user) (:month user) (:year user)))))

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
  []
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
  "Function to delete Birthday Entries all together (manage duplicates, un-friend people etc.)"
  []
  (p/let [_answers (inquirer/prompt delete-questions)
          answers (js->clj _answers :keywordize-keys true)
          {:keys [:personID]} answers]
         (delete-birthday personID)))

(defn search-birthdays-by-day
  "Function to search for Birthdays on a specific day"
  [day month]
  (let [s_query (.prepare db "SELECT * FROM people AS p, gift_ideas AS g WHERE day=? AND month=? AND p.personID=g.personID ORDER BY sname ASC")
        s_resp (.all s_query day month)
        s_res (js->clj s_resp :keywordize-keys true)]
    (doseq [res s_res]
      (println "It's" (str (:name res) "'s") "Birthday on the" (str (:day res)) "of" (str (:month res)) "and they want a" (:gift_idea res) "🏆"))))

(defn search-birthdays-by-month
  "Function to search for Birthdays in a specific month"
  [month]
  (let [s_query (.prepare db "SELECT * FROM people AS p, gift_ideas AS g WHERE month=? AND p.personID=g.personID ORDER BY day,sname ASC")
        s_resp (.all s_query month)
        s_res (js->clj s_resp :keywordize-keys true)]
    (prn-str s_res)
    (doseq [res s_res]
      (println "It's" (str (:name res) "'s") "Birthday on the" (str (:day res)) "of" (str (:month res)) "and they want a" (:gift_idea res) "🏆"))))

(defn help-message
  "Function to display help and other on-invocation messages"
  [mode]
  (cond
    (= mode "help") (println (str (fs/readFileSync "./help.txt")))
    (= mode "invalid") (println (str (fs/readFileSync "./invalid.txt")))
    :else (println "Unknown Help Mode")))

(cond
  (= (first *command-line-args*) "list") (list-birthdays)
  (= (first *command-line-args*) "list-people") (list-people)
  (= (first *command-line-args*) "update") (update-birthday-entry)
  (= (first *command-line-args*) "delete") (delete-birthday-entry)
  (= (first *command-line-args*) "search-day") (search-birthdays-by-day
                                                (second *command-line-args*)
                                                (last *command-line-args*))
  (= (first *command-line-args*) "search-month") (search-birthdays-by-month
                                                  (last *command-line-args*))
  (= (first *command-line-args*) "help") (help-message "help")
  :else (if (= 0 (count *command-line-args*))
          (create-birthday-entry)
          (help-message "invalid")))
