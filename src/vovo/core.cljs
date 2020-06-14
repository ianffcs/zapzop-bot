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

(defn filter-contacts [contacts n]
  (->> contacts
     (filter #(= n (gobj/get % "formattedName")))
     (map #(gobj/get % "id"))
     first))

(defn send-image-to-contact! [future-client future-contact future-img]
  (let [p (async/promise-chan)
        _ (prn [:outside-go future-contact (count (str future-img))])]
    (async/go
      (let [promise (.sendFile future-client
                               future-contact
                               future-img
                               "hellooo.jpeg"
                               "test bot")]
        (.then promise (fn [v]
                         (prn [:then v])
                         (async/>! p v)))
        (.catch promise (fn [v]
                          (prn [:error v])
                          (async/>! p v)))))
    p))

(defn pic-gen-url []
  (str "https://picsum.photos/1000?random=" (.random js/Math)))

(defn main []
  (let [contact-list  ["person1" "person2"]
        contacts+urls (vec (for [contact contact-list]
                             [contact (pic-gen-url)]))]
    (prn "begin")
    (async/go
      (let [client   (<p! (wa/create))
            contacts (async/<! (async/go (<p! (.getAllContacts client))))
            #_#__    (prn [:inside-go (str (count img)) (count contacts)])]
        (doseq [[contact url] contacts+urls]
          (send-image-to-contact! client
                                  (filter-contacts contacts contact)
                                  (async/<! (place-text-on-image! url))))))
    (prn "finished")))

#_(go (->> (get-image! pic-gen-url)
           async/<!
           prn))

#_(go (->> (jimp/loadFont jimp/FONT_SANS_128_WHITE)
           async/<!
           prn))

(defn before-load []
  #_(main)
  (prn "loading..."))
