(ns cloister.utils)

(defn deep-merge
  "Recursively merges maps. If vals are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defmacro let-keys
  "Macro that wraps let for {:keys []} destructuring of maps."
  [bindings & body]
  (let [into-keys (fn [[k v]] [{:keys k} v])
        key-bindings (->> bindings
                          (partition-all 2)
                          (mapcat into-keys))]
    `(let [~@key-bindings] ~@body)))

(defn flush!
  "Implementation of reset! that returns the old value instead
  of the new one"
  [atom newval]
  (let [val @atom]
    (if (compare-and-set! atom val newval)
      val
      (recur atom newval))))
