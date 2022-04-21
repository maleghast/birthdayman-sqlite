(ns app
  (:require ["inquirer$default" :as inquirer]
            ["moment$default" :as moment]
            [promesa.core :as p]
            ["sqlite3$default" :as sql]))

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
                         {:name "present-idea"
                          :type "input"
                          :message "What shall you get for them?"
                          :default "Beats the shit outta me!"}]))

(def db (sql/Database. ":memory:"))

(defn create-birthday-entry
  "Function to store Birthday Entry in DB"
  []
  (p/let [_answers (inquirer/prompt questions)
          answers (js->clj _answers :keywordize-keys true)]
    (prn answers)))

(cond
  (= (first *command-line-args*) "list") (prn "list birthdays")
  :else (create-birthday-entry))