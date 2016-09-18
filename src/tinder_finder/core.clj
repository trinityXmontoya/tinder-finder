(ns tinder-finder.core
  (:require [tinder-finder.tinder :as tinder]
            [tinder-finder.redis :as redis]
            [environ.core :refer [env]]
            [org.httpkit.client :as http]
            [cheshire.core :as json])
  (:gen-class))

; helper fns
; credit - http://stackoverflow.com/a/8056727/3481754
(defn dups [seq]
  (for [[id freq] (frequencies seq)
        :when (> freq 1)]
   id))

; match filter
(defn get-img-tags
  "query and parse Clarifai API for identifiying tags
   associated with img-url"
  [img-url]
  (let [api-url "https://api.clarifai.com/v1/tag/"
        opts {:headers {"Authorization" (str "Bearer " (env :clarifai-token))}
              :query-params {:url img-url}}
        {:keys [status body error]} @(http/get api-url opts)
        body (json/decode body true)]
    (if (not (= status 200))
      (throw error)
      (get-in body [:results 0 :result :tag :classes]))))

(defn img-match-found?
  "determine whether img-url is similar to stored image based off
   respective identifying tags"
  [img-url]
  (let [img-tags (get-img-tags img-url)
        search-tags (read-string (env :search-tags))
        duplicates (dups (concat img-tags search-tags))]
    (redis/save-tags img-tags)
    (=  (count search-tags) (count duplicates))))

(defn sort-recs
  "sort recommendations based off provided filters
   returns {:matches [match1 match2] :pass-id [pass1 pass2]}"
  [recs]
  (let [matches (transient [])
        pass-id (transient [])]
    (mapv (fn [r]
            (if (or (= (r :name) (env :search-name))
                    (contains? (mapv :name (r :schools)) (env :search-school))
                    (boolean (re-find (re-pattern (env :search-bio)) (r :bio)))
                    (img-match-found? ((first (r :photos)) :url)))
              (conj! matches r)
              (conj! pass-id (r :_id)))) recs)
    {:matches (persistent! matches) :pass-id (persistent! pass-id)}))

(defn process-recs []
  (let [sorted (sort-recs (tinder/get-recs))
        matches (sorted :matches)
        pass-id (sorted :pass-id)]
    (println "Matches:" (count matches) "Passes:" (count pass-id))
    (doseq [id pass-id]
      (tinder/pass-user id)
      (Thread/sleep 300))
    (when (> (count matches) 0)
      (redis/save-matches matches)
      ; optionally like user so they don't come back in results
      (map tinder/like-user (map :id matches)))))

(defn -main
  "Get catching"
  []
  (while true
    (process-recs)
    (Thread/sleep 1000)))
