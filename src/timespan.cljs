(ns timespan
  (:require [roam.datascript.reactive :as rd]
            [roam.util :refer [parse]]
            [clojure.string :as str]))

(def months [nil "January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"])

(defn title->date [title]
  (let [[m d y] (take-last 3
                         (re-find
                          #"(\w+) (\d+).+, (\d{4})"
                          title))]
  [(js/parseInt y) (.indexOf months m) (js/parseInt d)]))

(defn timespan->dates [str]
  (if-let [two-dates (re-find #"^timespan:: \[\[(.+)\]\].*\[\[(.+)\]\].*" str)]
    (->> two-dates
        (take-last 2)
        (map title->date))
    
      (->> str
        (re-find #"^timespan:: \[\[(.+)\]\]")
        (take-last 1)
        (map title->date))))

(defn within-timespan? [start end now]
  (and (<= (compare start now) 0)
       (<= (compare now end) 0)))

(defn main [{:keys [block-uid]}]
  (let [block (rd/pull '[* {:block/page [:node/title]}] [:block/uid block-uid])
        timespans (rd/q '[:find (pull ?parent [:block/uid]) (pull ?b [:block/string])
                          :where [?b :block/string ?string]
                                 [(re-matches #"^timespan:: \[\[.*\]\]" ?string)]
                                 [?parent :block/children ?b]])
        now   (-> @block :block/page :node/title)
        happenings (->> @timespans
        (filter (fn [[_ blk]]
                  (let [[start end] (->> blk :block/string timespan->dates)
                        now (title->date now)]
                    (within-timespan? start (or end now) now))))
        (map (comp #(str "((" % "))") :block/uid first)))]
    (if (empty? happenings)
      (parse "Nothing happening...")
      (->> happenings (str/join "\n\n") parse))))