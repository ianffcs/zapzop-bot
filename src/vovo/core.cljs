(ns vovo.core
  (:require ["jimp" :as jimp]
            ["@open-wa/wa-automate" :as wa]
            [goog.object :as gobj]
            [cljs.core.async :as async]
            [cljs.core.async.interop :refer-macros [<p!]]))

(def openings
  ["Bom dia"
   "Deus te abençoe"
   "Um ótimo dia"])

(def endings
  ["meu tesouro
   meu anjo
   nesta"])

(defn get-x-pos [width text-width]
  (/ (- width text-width) 2))

(defn place-top-text [image font top-text width]
  (let [text-width  (.measureText jimp font top-text)
        text-height (.measureTextHeight jimp  font top-text)]
    (.print image
            font
            (get-x-pos width text-width)
            (/ text-height 3)
            top-text
            width
            text-height)))

(defn place-bot-text [image font bottom-text height width]
  (let [text-width  (.measureText jimp font bottom-text)
        text-height (.measureTextHeight jimp font bottom-text)
        xpos        (/ (- width text-width) 2)]
    (.print image
            font
            xpos
            (* 2 (- height text-height))
            bottom-text
            width
            text-height)))

(defn place-text-on-image! [url]
  (async/go (let [image   (<p! (jimp/read url))
                  width   (.getWidth image)
                  height  (.getHeight image)
                  font128 (jimp/loadFont jimp/FONT_SANS_128_WHITE)
                  font64  (jimp/loadFont jimp/FONT_SANS_64_BLACK)]
              (-> image
                 (place-top-text (<p! font128)
                                 (rand-nth openings)
                                 height)
                 (place-bot-text (<p! font64)
                                 (rand-nth endings)
                                 height
                                 width)
                 (.getBase64Async (.-MIME_JPEG jimp))
                 <p!))))

(defn get-contacts! [future-client]
  (async/go
    (<p! (.getAllContacts future-client))))

(defn get-contact-id [contacts n] ;; see what happens here
  (->> contacts
     (filter #(= n (gobj/get % "formattedName")))
     (keep #(gobj/get % "id"))
     first))

(defn get-contacts-id [contacts name-list]
  (keep #(get-contact-id contacts %) name-list))

(defn pic-gen-url []
  (str "https://picsum.photos/1000?random=" (.random js/Math)))

(defn send-image-to-contact! [future-client
                              future-contact-id
                              future-img]
  (prn [:outside-go future-contact-id (count (str future-img))])
  (let [sent (.sendFile future-client
                        future-contact-id
                        future-img
                        "hellooo.jpeg"
                        "test bot")]
    (async/go (<p! sent))))

(defn send-image-to-contacts! [future-client future-contact-ids]
  (let [ids+urls (vec (for [id future-contact-ids]
                        [id (pic-gen-url)]))
        p        (async/promise-chan)]
    (prn ids+urls)
    (async/go
      (doseq [[id url] ids+urls]
        (async/<! (send-image-to-contact!
                   future-client
                   id
                   (async/<! (place-text-on-image! url))))))
    p))

(defn send-text! [future-client future-contact-id msg]
  (prn [:outside-go future-contact-id msg])
  (let [sent (.sendText future-client future-contact-id msg)]
    (async/go (<p! sent))))

(defn main []
  (let [name-list ["person1"
                   "person2"
                   "person3"]]
    (prn "begin")
    (async/go
      (let [client   (<p! (wa/create))
            contacts (async/<! (get-contacts! client))
            ids      (get-contacts-id contacts name-list)]
        (prn ids)
        #_(prn (async/<! (send-text! client (second ids) "aa")))
        #_(prn (async/<! (send-image-to-contacts! client ids)))
        ))
    (prn "finished")))

(comment
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
      (.then p (fn [ok]
                 (fn [ok]
                   (async/put! chan (if (nil? ok)
                                      ::nil
                                      ok)))))
      chan))
  (defn async->callback
    [chan on-ok]
    (async/go
      (on-ok (async/<! chan)))))

#_(go (->> (get-image! pic-gen-url)
           async/<!
           prn))

#_(go (->> (jimp/loadFont jimp/FONT_SANS_128_WHITE)
           async/<!
           prn))

(defn before-load []
  #_(main)
  (prn "loading..."))
