(ns reactive.graph
  (:require ["d3" :as d3]))

(def canvas (-> js/document (.getElementById "canvas")))
(def ctx (-> canvas (.getContext "2d")))
(def node-radius 35)
(def distance 180)

(defn force-simulation
  [width height]
  (-> d3
      (.forceSimulation)
      (.force "center" (-> d3 (.forceCenter (/ width 2) (/ height 2))))
      (.force "link" (-> d3 (.forceLink) (.id (fn [d] (-> d .-id)))))
      (.force "change" (-> d3 (.forceManyBody)))))

(def simulation (force-simulation (.-width canvas) (.-height canvas)))

(defn get-distance
  [x1 y1 x2 y2]
  (js/Math.sqrt (+ (js/Math.pow (- x2 x1) 2) (js/Math.pow (- y2 y1) 2))))

(defn find-point
  [x1 y1 x2 y2 distance1 distance2]
  {:x (- x2 (/ (* distance2 (- x2 x1)) distance1))
   :y (- y2 (/ (* distance2 (- y2 y1)) distance1))})

(defn draw-edges
  [edge]
  (let [source-x (-> edge .-source .-initial_pos .-x)
        source-y (-> edge .-source .-initial_pos .-y)
        target-x (-> edge .-target .-initial_pos .-x)
        target-y (-> edge .-target .-initial_pos .-y)
        target-index (-> edge .-target .-index)
        source-index (-> edge .-source .-index)
        dis-betw-edges (/ node-radius 2)
        edge-pos (if (< target-index source-index) dis-betw-edges (- dis-betw-edges))
        value (-> edge .-value)
        point-dis (get-distance
                    source-x
                    source-y
                    target-x
                    target-y)
        weight-point (find-point
                       source-x
                       source-y
                       target-x
                       target-y
                       point-dis
                       (/ point-dis 2))]
    (doto ctx
      (.save)
      ((fn [v] (set! (.-globalAlpha v) 0.7)))
      (.translate 0 edge-pos)
      (.beginPath)
      (.moveTo source-x source-y)
      (.lineTo target-x target-y)
      ((fn [v] (set! (.-lineWidth v) (js/Math.sqrt value))))
      ((fn [v] (set! (.-strokeStyle v) "black")))
      (.stroke)
      ((fn [v] (set! (.-globalAlpha v) 1)))
      ; ((fn [v] (set! (.-font v) "bold 18px sans-serif")))
      ; ((fn [v] (set! (.-textBaseline v) "middle")))
      ; ((fn [v] (set! (.-fillStyle v) "blue")))
      ; ((fn [v] (set! (.-textAlign v) "center")))
      ; (.fillText value (weight-point :x) (weight-point :y))
      (.restore)
      )))

(defn draw-nodes
  [node]
  (let [x-initial-pos (-> node .-initial_pos .-x)
        y-initial-pos (-> node .-initial_pos .-y)]
    (doto ctx
      (.beginPath)
      (.moveTo (+ x-initial-pos node-radius) y-initial-pos)
      (.arc x-initial-pos y-initial-pos node-radius 0 (* 2 js/Math.PI))
      ((fn [v] (set! (.-fillStyle v) "black")))
      (.fill)
      ((fn [v] (set! (.-font v) "700 22px sans-serif")))
      ((fn [v] (set! (.-fillStyle v) "white")))
      ((fn [v] (set! (.-textAlign v) "center")))
      ((fn [v] (set! (.-textBaseline v) "middle")))
      (.fillText (-> node .-id) x-initial-pos y-initial-pos)
      ((fn [v] (set! (.-strokeStyle v) "#fff")))
      ((fn [v] (set! (.-lineWidth v) "1.5")))
      (.stroke))))

(defn draw-graph
  [edges nodes]
  (js/console.log "simulation on...")
  (doto ctx
    (.save)
    (.clearRect 0 0 (.-width canvas) (.-height canvas)))
  (doseq [e edges] (draw-edges e))
  (doseq [n nodes] (draw-nodes n))
  (-> ctx (.restore)))

(defn force-graph
  [data]
  (let [nodes (-> data .-nodes)
        edges (-> data .-links)]

    (-> simulation
        (.nodes nodes)
        (.on "tick" (fn [] (draw-graph edges nodes))))

    (-> simulation
        (.force "link")
        (.links edges))))

(defn place-node
  [x-por y-por]
  #js {:x (* (.-width canvas) (/ x-por 100))  :y (* (.-height canvas) (/ y-por 100))})

; TODO: estabelecer posicionamento inicial dos nodes
; https://bl.ocks.org/mbostock/3750558
; https://tsj101sports.com/2018/06/20/football-with-graph-theory/
(def mock-data
  {
   :nodes [
           {:id "7" :group 1 :initial_pos (place-node 30 6)}
           {:id "9" :group 1 :initial_pos (place-node 70 6)}
           {:id "6" :group 1 :initial_pos (place-node 9 28)}
           {:id "14" :group 1 :initial_pos (place-node 50 28)}
           {:id "8" :group 1 :initial_pos (place-node 91 28)}
           {:id "6" :group 1 :initial_pos (place-node 9 28)}
           {:id "16" :group 1 :initial_pos (place-node 50 58)}
           {:id "15" :group 1 :initial_pos (place-node 91 58)}
           {:id "11" :group 1 :initial_pos (place-node 9 58)}
           {:id "3" :group 1 :initial_pos (place-node 72 77)}
           {:id "5" :group 1 :initial_pos (place-node 28 77)}
           {:id "1" :group 1 :initial_pos (place-node 50 95)}
           ]
   :links [
           {:source "3" :target "1" :value 1}
           {:source "1" :target "3" :value 100}
           {:source "5" :target "1" :value 23}
           {:source "1" :target "5" :value 2}
           {:source "1" :target "7" :value 2}
           ]
   })

; https://observablehq.com/d/42f72efad452c2f0
; (defn init-graph [] (-> get-data (.then force-graph)))
(defn init-graph [] (-> mock-data clj->js force-graph))
