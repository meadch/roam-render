(ns refs
  (:require
   [reagent.core :as r]
   [roam.datascript.reactive :as rd]
   [roam.util :refer [parse]]))

(defn wrap-parens [x]
  (str "((" x "))"))

(defn ident->refs [idents]
  (rd/q
   '[:find ?ref-target ?uid
     :in $ [?ident ...]
     :where
     [(untuple ?ident) [?a ?v]]
     [?parent ?a ?v]
     (or
      [?cid ?a ?v]
      [?cid :block/parents ?parent])
     [?cid :block/uid ?ref-target]
     [?refid :block/refs ?cid]
     [?refid :block/uid ?uid]]
   idents))

(defn main [_ & idents]
  (r/with-let [*uids (ident->refs idents)
               reffed (->> @*uids (group-by first) vals set)]
    (->> @*uids
           (group-by first)
           (reduce (fn [a [k col]]
                     (assoc a k (map (comp second) col))) {})
           (mapcat (fn [[k col]]
                     (concat [(wrap-parens k)] 
                             (->> col
                               (remove reffed)
                               (map wrap-parens)
                               (interleave (repeat "\n\n")))
                             ["---"])))
           (map parse)
           (into [:<>]))))