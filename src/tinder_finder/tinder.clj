(ns tinder-finder.tinder
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [taoensso.carmine :as car :refer (wcar)]
            [taoensso.timbre :as timbre]
            [environ.core :refer [env]])
  (:gen-class))

(timbre/refer-timbre)

; conf
(def headers
  {"Content-Type" "application/json"
   "user-agent" "Tinder/4.1.4 (iPhone; iOS 8.1.3; Scale/2.00)"
   "X-Auth-Token" (env :tinder-token)})
(def api-url "https://api.gotinder.com")

;  calls
(defn tinder-api-wrap [method path opts]
  (let [url (str api-url path)
        opts (merge {:headers headers} opts)
        {:keys [status body error]} @(http/request (merge {:method method :url url} opts))
        body (json/decode body true)]
    (if-not (= status 200)
      (throw error)
      body)))

(defn set-preferences [gender gender-filter age-min age-max dist-max]
  (let [opts {:body (json/encode {:gender gender
                                  :gender-filter gender-filter
                                  :age-filter-min age-min
                                  :age-filter-max age-max
                                  :distanct-filter dist-max})}]
    (tinder-api-wrap :post "/profile" opts)))

(defn set-location [lat lng]
  (let [opts {:body (json/encode {:lat lat
                                  :lng lng})}]
    (tinder-api-wrap :post "/user/ping" opts)))

(defn get-recs []
  (let [res (tinder-api-wrap :get "/user/recs" {})]
    (res :results)))

(defn pass-user [id]
  (let [path (str "/pass/" id)]
    (tinder-api-wrap :get path {})))

(defn like-user [id]
  (let [path (str "/like/" id)]
    (tinder-api-wrap :get path {})
      (Thread/sleep 1000)))
