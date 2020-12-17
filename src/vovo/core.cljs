(ns vovo.core
  (:require ["jimp" :as jimp]
            ["@open-wa/wa-automate" :as wa]
            ["fs/promises" :as fs]
            [goog.object :as gobj]
            [cljs.core.async :as async :refer [<! >!]]
            [com.wsscode.async.async-cljs :refer [go-promise <? <!p]]))

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

#_(defn place-text-on-image! [{:keys [openings
                                      endings]} url]
    (async/go (let [image   (<!p (jimp/read url))
                    width   (.getWidth image)
                    height  (.getHeight image)
                    font128 (jimp/loadFont jimp/FONT_SANS_128_WHITE)
                    font64  (jimp/loadFont jimp/FONT_SANS_64_BLACK)]
                (-> image
                    (place-top-text (<!p font128)
                                    (rand-nth openings)
                                    height)
                    (place-bot-text (<!p font64)
                                    (rand-nth endings)
                                    height
                                    width)
                    (.getBase64Async (.-MIME_JPEG jimp))
                    <!p))))

(defn pic-gen-url []
  (str "https://picsum.photos/1000?random=" (.random js/Math)))

#_(defn send-image-to-contact! [future-client
                                future-contact-id
                                future-img]
    (prn [:outside-go future-contact-id (count (str future-img))])
    (let [sent (.sendFile future-client
                          future-contact-id
                          future-img
                          "hellooo.jpeg"
                          "test bot")]
      (async/go (<!p sent))))

#_(defn send-image-to-contacts! [config-map future-client future-contact-ids]
    (let [ids+urls (vec (for [id future-contact-ids]
                          [id (pic-gen-url)]))
          p        (async/promise-chan)]
      (prn ids+urls)
      (async/go
        (doseq [[id url] ids+urls]
          (async/<! (send-image-to-contact!
                     future-client
                     id
                     (async/<! (place-text-on-image! config-map url))))))
      p))

(defn get-all-contacs [client]
  (.getAllContacts ^js client))

(defn send-text [client {:keys [id msg]}]
  (.sendText ^js client id msg))

(defn send-image [client {:keys [id
                                 file
                                 filename
                                 caption
                                 quotedMsgId?
                                 waitForId?
                                 withoutPreview?]}]
  (.sendImage ^js client id file filename caption))

(defn name->id [contacts name] ;; see what happens here
  (->> contacts
       (filter #(= name (gobj/get % "formattedName")))
       (keep #(gobj/get % "id"))
       first))

(defn names->ids [contacts names]
  (keep #(name->id contacts %) names))

(defn main []
  (go-promise
   (let [{:keys [names
                 msgs]} (or (async/go (-> "example-input.json"
                                          fs/readFile
                                          <!p
                                          js/JSON.parse
                                          (js->clj :keywordize-keys true)))
                            #_config-map)
         client (<!p (wa/create))
         contacts (<!p (get-all-contacs client))
         ids (names->ids contacts names)]
     (prn "begin")
     (prn names)
     (prn ids)
     (prn msgs)
     #_(doseq [id ids
               msg msgs]
         (<!p  (send-text client {:id id
                                  :msg msg})))
     #_(doseq [id ids]
         (<!p (send-image client {:id id
                                  :file "/home/ianffcs/Imagens/anarchydoggo.jpg"
                                  :filename "anarchydoggo.jpg"
                                  :caption "AUTO: doggo"})))
     (prn "finished"))))

#_(async/go (<!p (utils/read-config-map "./example-input.json")))
#_(go (->> (get-image! pic-gen-url)
           async/<!
           prn))

#_(go (->> (jimp/loadFont jimp/FONT_SANS_128_WHITE)
           async/<!
           prn))

(defn ^:dev/before-load before-load []
  #_(main)
  (prn "loading..."))
