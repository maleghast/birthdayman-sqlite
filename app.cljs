(ns app
  (:require ["better-sqlite3$default" :as sql]
            ["inquirer$default" :as inquirer]
            ["moment$default" :as moment]
            [promesa.core :as p]))

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
                         {:name "gift-idea"
                          :type "input"
                          :message "What shall you get for them?"
                          :default "Beats the shit outta me!"}]))

(def db (sql. "birthdays.db"))

(defn write-birthday
  "Function to persist a Birthday Record"
  [name day month gift-idea]
  (p/let [p_query (.prepare db "INSERT INTO people(name, day, month) VALUES (?,?,?)")
          p_resp (.run p_query name day month)
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
          {:keys [name day month gift-idea]} answers]
    (write-birthday name day month gift-idea)))

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

(cond
  (= (first *command-line-args*) "list") (list-birthdays)
  :else (create-birthday-entry))