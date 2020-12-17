(ns vovo.whats-old)

#_(defn get-x-pos [width text-width]
    (/ (- width text-width) 2))

#_(defn place-top-text [image font top-text width]
    (let [text-width  (.measureText jimp font top-text)
          text-height (.measureTextHeight jimp  font top-text)]
      (.print image
              font
              (get-x-pos width text-width)
              (/ text-height 3)
              top-text
              width
              text-height)))

#_(defn place-bot-text [image font bottom-text height width]
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
