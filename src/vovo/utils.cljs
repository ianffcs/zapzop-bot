(ns vovo.utils
  (:require [cljs.core.async :as async]))

(defn callback->promise
  [f]
  (let [p (new js/Promise)]
    (f (fn [ok]
         (.resolve p ok))
       (fn [err]
         (.reject p err)))))

 (defn callback->async
   [f]
   (let [p (async/promise-chan)]
     ;; core.async do not handle exceptions nor nil as value
     (f (fn [ok]
          (async/put! p (if (nil? ok)
                          ::nil
                          ok))))
     p))
 (defn promise->async
   [p]
   (let [chan (async/promise-chan)]
     (.then p  (fn [ok]
                 (async/put! chan (if (nil? ok)
                                    ::nil
                                    ok))))
     chan))

(defn async->callback
  [chan on-ok]
  (async/go
    (on-ok (async/<! chan))))
