; Inspiração:
; https://tsj101sports.com/2018/06/20/football-with-graph-theory/
(ns reactive.graph
  (:require
    ["d3" :as d3]
    [reactive.utils :refer [get-distance find-point radians-between]]))

(def canvas (-> js/document (.getElementById "canvas")))
(def ctx (-> canvas (.getContext "2d")))
(def node-radius 35)
(def edges-padding 10)
(def edges-alpha 0.5)

(defn force-simulation
  [width height]
  (-> d3
      (.forceSimulation)
      (.force "center" (-> d3 (.forceCenter (/ width 2) (/ height 2))))
      (.force "link" (-> d3 (.forceLink) (.id (fn [d] (-> d .-id)))))
      (.force "change" (-> d3 (.forceManyBody)))))

(def simulation (force-simulation (.-width canvas) (.-height canvas)))

(defn draw-edges
  [edge]
  (let [source-x (-> edge .-source .-initial_pos .-x)
        source-y (-> edge .-source .-initial_pos .-y)
        target-x (-> edge .-target .-initial_pos .-x)
        target-y (-> edge .-target .-initial_pos .-y)
        dis-betw-edges (/ node-radius 2)
        value (-> edge .-value)
        point-between (partial find-point source-x source-y target-x target-y)
        source-target-distance (get-distance
                                 source-x
                                 source-y
                                 target-x
                                 target-y)
        base-vector [source-target-distance 0]
        target-vector [(- target-x source-x) (- target-y source-y)]

        ; calculate angle of target projetion align with source along the x-axis
        radians (radians-between base-vector target-vector)
        orientation (cond
                      (and (< source-x target-x) (< source-y target-y)) radians
                      (and (> source-x target-x) (< source-y target-y)) radians
                      (and (= source-x target-x) (< source-y target-y)) radians
                      :else (- radians))]

    (doto ctx
      ((fn [v] (set! (.-globalAlpha v) edges-alpha)))
      ; translate to source node center point
      (.translate source-x source-y)
      ; rotate canvas by that angle
      (.rotate orientation)
      ; translate again between edges
      (.translate 0 dis-betw-edges)
      ; draw edges
      (.beginPath)
      (.moveTo (-> node-radius (+ edges-padding)) 0)
      (.lineTo (-> base-vector first (- node-radius edges-padding)) (second base-vector))
      ((fn [v] (set! (.-lineWidth v) (js/Math.sqrt value))))
      ((fn [v] (set! (.-strokeStyle v) "black")))
      (.stroke)
      ; restore canvas
      (.setTransform))))

(defn draw-passes
  [edge]
  (-> ctx (.save))
  (draw-edges edge)
  (-> ctx (.restore)))

(defn draw-numbers
  [node]
  (let [x-initial-pos (-> node .-initial_pos .-x)
        y-initial-pos (-> node .-initial_pos .-y)]
    (doto ctx
      ((fn [v] (set! (.-font v) "700 22px sans-serif")))
      ((fn [v] (set! (.-fillStyle v) "white")))
      ((fn [v] (set! (.-textAlign v) "center")))
      ((fn [v] (set! (.-textBaseline v) "middle")))
      (.fillText (-> node .-id) x-initial-pos y-initial-pos))))

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
      ((fn [v] (set! (.-strokeStyle v) "#fff")))
      ((fn [v] (set! (.-lineWidth v) "1.5")))
      (.stroke))))

(defn draw-players
  [node]
  (doto node
    (draw-nodes)
    (draw-numbers)))

(defn draw-graph
  [edges nodes]
  (doto ctx
    (.save)
    (.clearRect 0 0 (.-width canvas) (.-height canvas))
    ((fn [v] (set! (.-fillStyle v) "white")))
    (.fillRect 0 0 (.-width canvas) (.-height canvas)))
  (doseq [e edges] (draw-passes e))
  (doseq [n nodes] (draw-players n))
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
  [x-% y-%]
  #js {:x (* (.-width canvas) (/ x-% 100))  :y (* (.-height canvas) (/ y-% 100))})

(def mock-edges (for [source ["1" "5" "3" "11" "15" "16" "6" "8" "14" "6" "9" "7"]
                      target ["1" "5" "3" "11" "15" "16" "6" "8" "14" "6" "9" "7"]
                      :let [edge {:source source
                                  :target target
                                  :value (if (zero? (rand-int 2)) 1 50)}]
                      :when (not= source target)]
                  edge))

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
   :links (-> mock-edges vec)
   ; :links [
   ;         {:source "6" :target "14" :value 1}
   ;         {:source "14" :target "6" :value 100}
   ;         {:source "8" :target "14" :value 1}
   ;         {:source "14" :target "8" :value 100}
   ;         {:source "6" :target "1" :value 1}
   ;         {:source "1" :target "6" :value 100}
   ;         {:source "3" :target "1" :value 1}
   ;         {:source "1" :target "3" :value 100}
   ;         {:source "5" :target "1" :value 23}
   ;         {:source "1" :target "5" :value 2}
   ;         {:source "1" :target "7" :value 2}
   ;         {:source "7" :target "1" :value 2}
   ;         {:source "1" :target "16" :value 2}
   ;         {:source "16" :target "1" :value 2}
   ;         {:source "5" :target "11" :value 2}
   ;         {:source "11" :target "5" :value 2}
   ;         {:source "9" :target "11" :value 2}
   ;         {:source "11" :target "9" :value 2}
   ;         {:source "6" :target "11" :value 2}
   ;         {:source "11" :target "6" :value 2}
   ;         {:source "8" :target "11" :value 2}
   ;         {:source "11" :target "8" :value 2}
   ;         {:source "3" :target "11" :value 2}
   ;         {:source "11" :target "3" :value 2}
   ;         {:source "7" :target "11" :value 2}
   ;         {:source "11" :target "7" :value 2}
   ;         {:source "15" :target "11" :value 2}
   ;         {:source "11" :target "15" :value 2}
   ;         {:source "15" :target "8" :value 2}
   ;         {:source "8" :target "15" :value 2}
   ;         {:source "15" :target "5" :value 2}
   ;         {:source "5" :target "15" :value 2}
   ;         {:source "1" :target "9" :value 2}
   ;         {:source "9" :target "1" :value 2}
   ;         {:source "14" :target "9" :value 2}
   ;         {:source "9" :target "14" :value 2}
   ;         {:source "15" :target "3" :value 2}
   ;         {:source "3" :target "15" :value 2}
   ;         {:source "15" :target "14" :value 2}
   ;         {:source "14" :target "15" :value 2}
   ;         {:source "16" :target "6" :value 2}
   ;         {:source "6" :target "16" :value 2}
   ;         ]
   })

; https://observablehq.com/d/42f72efad452c2f0
(defn init-graph [] (-> mock-data clj->js force-graph))
