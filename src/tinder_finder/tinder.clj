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
(def test-recs (json/decode (env :search-sample) true))

;  calls
(defn get-auth-token
  []
  (let [url (str api-url "/auth")
        opts {:headers {"Content-Type" "application/json"
                        "User-Agent" "Tinder/4.1.4 (iPhone; iOS 8.1.3; Scale/2.00)"}
              :body (json/encode {:facebook_token (env :fbook-token)
                                  :facebook_id (env :fbook-id)})}]
    (http/post url opts
      (fn [{:keys [status headers body err]}]
        (let [res (json/decode body true)]
        (if-not (= 200 status)
          (error "processing response" res)
          (res :token)))))))

(defn set-preferences
  [gender gender-filter age-min age-max dist-max]
  (let [url (str api-url "/profile")
        opts {:headers headers
              :body (json/encode {:gender gender
                                  :gender-filter gender-filter
                                  :age-filter-min age-min
                                  :age-filter-max age-max
                                  :distanct-filter dist-max})}]
      (http/post url opts
        (fn [{:keys [status headers body err]}]
          (let [res (json/decode body true)]
          (if-not (= 200 status)
            (error "processing response" res)
            res))))))

(defn set-location
  [lat lng]
  (let [url (str api-url "/user/ping")
        opts {:headers headers
              :body (json/encode {:lat lat
                                  :lng lng})}]
      (http/post url opts
        (fn [{:keys [status headers body err]}]
          (let [res (json/decode body true)]
          (if-not (= 200 status)
            (error "processing response" res))
          res)))))

(defn get-recs
  []
  (let [url (str api-url "/user/recs")
        opts {:headers headers}]
      (http/get url opts
        (fn [{:keys [status headers body err]}]
          (let [res (json/decode body true)]
          (if-not (= 200 status)
            (error "processing response" res)
            (res :results)))))))

(defn pass-user
  [id]
  (let [url (str api-url "/pass/" id)
        opts {:headers headers}]
    (http/get url opts
      (fn [{:keys [status headers body err]}]
        (let [res (json/decode body true)]
        (if-not (= 200 status)
          (error "processing response" status body)
          res))))))
