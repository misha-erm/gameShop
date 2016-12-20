(ns shop.core
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :as async :refer (<! >! put! chan)]
            [taoensso.sente :as sente :refer (cb-success?)]
            [secretary.core :as sec
                :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [accountant.core :as accountant]
            [shop.home :refer [homeView]]
            [shop.login]
            [ajax.core :refer [GET POST json-response-format]]
            [shop.ls])
  (:import goog.History))

(enable-console-print!)
; (accountant/configure-navigation! {:nav-handler (fn [path]
;   (sec/dispatch! path)) :path-exists? (fn [path]
;   (sec/locate-route path))})
(sec/set-config! :prefix "#")
(let [history (History.)
      navigation EventType/NAVIGATE]
  (goog.events/listen history
                     navigation
                     #(-> % .-token sec/dispatch!))
  (doto history (.setEnabled true)))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk" ; Note the same path as before
       {:type :auto ; e/o #{:auto :ajax :ws}
       })]
  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
)

(defonce app-state (atom {:user {}
                          :games []}))
(defn global-state []
    (om/ref-cursor (om/root-cursor app-state)))


; (defn editable-text-view
;     [state owner {:keys [state-key]}]
;     (reify
;         om/IInitState
;         (init-state [_]
;             {:editable false
;                 :temp-value nil})
;         om/IRenderState
;         (render-state [_ {:keys [editable temp-value]}]
;             (letfn [(cancel []
;                 (om/set-state! owner :editable false)
;                 )
;             (save []
;                 (om/update! state state-key temp-value)
;                 (om/set-state! owner :editable false))])
;             (if editable
;                 (dom/input #js {:value temp-value
;                     :onKeyDown (fn[e]
;                         (let [key (.-key e)]
;                             (case key
;                                 "Escape" (om/set-state! owner :editable false)
;                                 "Enter" ((om/update! state state-key temp-value)
;                                         (om/set-state! owner :editable false))
;                             nil)))
;                     :onChange (fn[e]
;                         (om/set-state! owner :temp-value (.-value (.-target e))))})
;                 (dom/div #js {:onClick (fn [e]
;                     (om/set-state! owner :temp-value (state-key state))
;                     (om/set-state! owner :editable true))}
;                 (state-key state))))))



; (defn app-view
;     [state owner]
;     (reify
;         om/IRender
;         (render [_]
;             (dom/div nil
;                 (om/build users-list-view (:users state))))))

(sec/defroute login-page "/login" []
    (om/root shop.login/login-view
        app-state
        {:target (.getElementById js/document "app")}))

; (sec/defroute index-page "/users" []
;     (om/root app-view
;         app-state
;         {:target (.getElementById js/document "app")}))

(sec/defroute home-page "/home" []
    (om/root homeView
        app-state
        {:target (.getElementById js/document "app")}))

(defn main []
  (let [token (shop.ls/get-item "token")]
    (if token
      (POST "/verifyToken" {:format :json
                            :response-format (json-response-format {:keywords? true})
                            :headers {"Authorization" (str "Token " token)}
                            :handler (fn [response]
                              (om/update! (global-state) [:user] (:user response))
                              (-> js/document
                                .-location
                                (set! "#/home")))})
      (-> js/document
        .-location
        (set! "#/login")))))
    

(main)