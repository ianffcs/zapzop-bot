(ns vovo.core
  (:require ["jimp" :as jimp]
            ["@open-wa/wa-automate" :as wa :refer [create]]
            [goog.object :as gobj]
            [cljs.core.async :as async]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]))

(def pic-gen-url
  (str "https://picsum.photos/1000?random=" (.random js/Math)))

(def openings
  ["Bom dia"
   "Deus te abençoe"
   "Um ótimo dia"])

(def endings
  ["meu tesouro"
   "meu anjo"
   "nesta"])

(defn get-x-pos [width text-width]
  (/ (- width text-width) 2))

(defn place-top-text [image font top-text width]
  (let [text-width  (.measureText jimp font top-text)
        text-height (.measureTextHeight jimp  font top-text)
        #_#_width   (gobj/get image "width")]
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
  (go (let [image   (<p! (jimp/read url))
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

(defn find-contacts! [client n]
  (go (let [contacts (<p! (.getAllContacts client))]
        (->> contacts
           (filter #(re-find (re-pattern n) (gobj/get % "formattedName")))
           (map #(gobj/get % "id"))
           (map #(gobj/get % "_serialized"))))))

(defn main []
  (go (let [client (<p! (wa/create))
            image  (place-text-on-image! pic-gen-url) ]
        #_(-> (find-contacts! client "Renata🖤")
              async/<!
              prn)
        #_(-> image async/<! boolean)
        (.sendFile client
                   (-> (find-contacts! client "Pessoa")
                      async/<!
                      first)
                   (async/<! image)
                   "oooiii.jpeg"
                   "testando o bot no cljs"))))

#_(go (->> (get-image! pic-gen-url)
           async/<!
           prn))

#_(go (->> (jimp/loadFont jimp/FONT_SANS_128_WHITE)
           async/<!
           prn))

(defn before-load []
  #_(main)
  (prn "loading..."))
