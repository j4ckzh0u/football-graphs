(ns football.metrics-nav
  (:require ["rxjs" :as rx]
            ["rxjs/operators" :as rx-op]))

(set! *warn-on-infer* true)

(defn select-metrics$
  []
  (let [node-color-select (-> js/document (.querySelector (str "[data-metric='node-color']")))
        node-area-select (-> js/document (.querySelector (str "[data-metric='node-area']")))
        coverage-select (-> js/document (.querySelector (str "[data-metric='coverage']")))
        position-select (-> js/document (.querySelector (str "[data-metric='position']")))
        min-passes-input (-> js/document (.querySelector (str "[data-metric='min-passes-to-display']")))
        min-passes-span (-> js/document (.querySelector (str "[data-min-passes-value]")))
        display-passes (fn [{:keys [min-passes-to-display]}]
                         (set! (.-innerHTML min-passes-span) (str "(" min-passes-to-display ")")))
        is-global? (fn [v] (= v :global))
        get-metrics (fn [] {:node-color-metric (-> node-color-select .-value keyword)
                            :node-radius-metric (-> node-area-select .-value keyword)
                            :position-metric (-> position-select .-value keyword)
                            :min-passes-to-display (-> min-passes-input .-value int)
                            :global-metrics? (-> coverage-select .-value keyword is-global?)})]
    (-> (rx/of
         node-color-select
         node-area-select
         coverage-select
         position-select
         min-passes-input)
        (.pipe
         (rx-op/mergeMap #(-> (rx/fromEvent % "input")
                              (.pipe (rx-op/map get-metrics))))
         (rx-op/startWith (get-metrics))
         (rx-op/tap display-passes)))))

(defn sticky-nav$
  []
  (let [menu (-> js/document (.querySelector ".nav-menu"))
        activate-btn (-> js/document (.querySelector "[data-active-metrics]"))
        deactivate-btn (-> js/document (.querySelector "[data-deactivate-metrics]"))
        nav (-> js/document (.querySelector ".nav-metrics"))
        breakpoint (-> js/document (.querySelector ".sticky-nav-breakpoint"))]

    (-> js/document
        (rx/fromEvent "scroll")
        (.pipe
         (rx-op/map (fn [] (-> breakpoint (.getBoundingClientRect) .-top (#(if (neg? %) 1 0)))))
         (rx-op/distinctUntilChanged))
        (.subscribe (fn [v]
                      (do
                        (-> menu (.setAttribute "data-sticky" v))))))
    (-> activate-btn
        (rx/fromEvent "click")
        (.subscribe (fn [_] (-> nav (.setAttribute "data-active" 1)))))

    (-> deactivate-btn
        (rx/fromEvent "click")
        (.subscribe (fn [_] (-> nav (.setAttribute "data-active" 0)))))))
