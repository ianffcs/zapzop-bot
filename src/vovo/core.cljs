(ns vovo.core
  (:require ["jimp" :as jimp]
            ["@open-wa/wa-automate" :as wa]
            ["fs/promises" :as fs]
            [goog.object :as gobj]
            [cljs.core.async :as async :refer [<! >!]]
            [com.wsscode.async.async-cljs :refer [go-promise <? <!p]]))

(defn get-all-contacs [client]
  (.getAllContacts ^js client))

(defn send-text! [client {:keys [id msg]}]
  (.sendText ^js client id msg))

(defn send-image! [client {:keys [id
                                  file
                                  filename
                                  caption
                                  quotedMsgId?
                                  waitForId?
                                  withoutPreview?]}]
  (.sendImage ^js client id file filename caption))

(defn name->id [contacts name]
  (->> contacts
       (filter #(= name (gobj/get % "formattedName")))
       (keep #(gobj/get % "id"))
       first))

(defn names->ids [contacts names]
  (keep #(name->id contacts %) names))

(defn main []
  (go-promise
   (let [{:keys [names
                 msgs]} (or config-map
                            (async/go (-> "example-input.json"
                                          fs/readFile
                                          <!p
                                          js/JSON.parse
                                          (js->clj :keywordize-keys true))))
         client (<!p (wa/create))
         contacts (<!p (get-all-contacs client))
         ids (names->ids contacts names)]
     (prn "begin")
     (prn names)
     (prn ids)
     (prn msgs)
     #_(doseq [id ids
               msg msgs]
         (<!p  (send-text! client {:id id
                                   :msg msg})))
     (prn "finished"))))

(defn ^:dev/before-load before-load []
  #_(main)
  (prn "loading..."))
