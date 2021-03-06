(ns kezban.core
  (:require [clojure.pprint :as pp]
            [clojure.walk :as walk]))

(defn- kezban
  []
  "I'M DAMN SEXY!")

(defmacro ^:private assert-all
  [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                  (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
       ~(let [more (nnext pairs)]
          (when more
            (list* `assert-all more)))))

(defmacro when-let*
  "Multiple binding version of when-let"
  [bindings & body]
  (when (seq bindings)
    (assert-all
      (vector? bindings) "a vector for its binding"
      (even? (count bindings)) "exactly even forms in binding vector"))
  (if (seq bindings)
    `(when-let [~(first bindings) ~(second bindings)]
       (when-let* ~(vec (drop 2 bindings)) ~@body))
    `(do ~@body)))

(defmacro if-let*
  "Multiple binding version of if-let"
  ([bindings then]
   `(if-let* ~bindings ~then nil))
  ([bindings then else]
   (when (seq bindings)
     (assert-all
       (vector? bindings) "a vector for its binding"
       (even? (count bindings)) "exactly even forms in binding vector"))
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-let* ~(vec (drop 2 bindings)) ~then ~else)
        ~else)
     then)))

(defmacro ->>>
  "Takes a set of functions and value at the end of the arguments.
   Returns a result that is the composition
   of those funtions.Applies the rightmost of fns to the args(last arg is the value/input!),
   the next fn (left-to-right) to the result, etc."
  [& form]
  `((comp ~@(reverse (rest form))) ~(first form)))

(defn drop-first
  "Return a lazy sequence of all, except first value"
  [coll]
  (drop 1 coll))

(defn nth-safe
  "Returns the value at the index. get returns nil if index out of
   bounds,unlike nth nth-safe does not throw an exception, returns nil instead.nth-safe
   also works for strings, Java arrays, regex Matchers and Lists, and,
   in O(n) time, for sequences."
  ([coll n]
   (nth-safe coll n nil))
  ([coll n not-found]
   (nth coll n not-found)))

(defn nnth
  [coll index & indices]
  (reduce #(nth-safe %1 %2) coll (cons index indices)))

(defn third
  "Gets the third element from collection"
  [coll]
  (nth-safe coll 2))

(defn fourth
  "Gets the fourth element from collection"
  [coll]
  (nth-safe coll 3))

(defn fifth
  "Gets the fifth element from collection"
  [coll]
  (nth-safe coll 4))

(defn sixth
  "Gets the sixth element from collection"
  [coll]
  (nth-safe coll 5))

(defn seventh
  "Gets the seventh element from collection"
  [coll]
  (nth-safe coll 6))

(defn eighth
  "Gets the eighth element from collection"
  [coll]
  (nth-safe coll 7))

(defn ninth
  "Gets the ninth element from collection"
  [coll]
  (nth-safe coll 8))

(defn tenth
  "Gets the tenth element from collection"
  [coll]
  (nth-safe coll 9))

(defn- xor-result
  [x y]
  (if (and x y)
    false
    (or x y)))

(defmacro xor
  ([] true)
  ([x] x)
  ([x & next]
   (let [first x
         second `(first '(~@next))
         ;; used this eval approach because of lack of private function usage in macro!
         result (xor-result (eval first) (eval second))]
     `(if (= (count '~next) 1)
        ~result
        (xor ~result ~@(rest next))))))

(defmacro pprint-macro
  [form]
  `(pp/pprint (walk/macroexpand-all '~form)))

(defn eval-when
  [test form]
  (when test (eval form)))

(defn has-ns?
  [ns]
  (try
    (the-ns ns)
    true
    (catch Exception _
      false)))

;;does not support syntax-quote
(defmacro quoted?
  [form]
  (if (coll? form)
    (let [f (first form)
          s (str f)]
      `(= "quote" ~s))
    false))

(defn- type->str
  [type]
  (case type
    :char "class [C"
    :byte "class [B"
    :short "class [S"
    :int "class [I"
    :long "class [J"
    :float "class [F"
    :double "class [D"
    :boolean "class [Z"
    nil))

(defn array?
  ([arr]
   (array? nil arr))
  ([type arr]
   (let [c (class arr)]
     (if type
       (and (= (type->str type) (str c)) (.isArray c))
       (or (some-> c .isArray) false)))))

(defmacro error?
  "Returns true if executing body throws an error(exception, error etc.), false otherwise."
  [& body]
  `(try
     ~@body
     false
     (catch Throwable _#
       true)))

(defn lazy?
  [x]
  (= "class clojure.lang.LazySeq" (str (type x))))

(defn any?
  [pred coll]
  ((complement not-any?) pred coll))

(defn ?
  [x]
  (if x true false))

(defn any-pred
  [& preds]
  (complement (apply every-pred (map complement preds))))

(defmacro try->
  [x & forms]
  `(try
     (-> ~x ~@forms)
     (catch Throwable _#)))

(defmacro try->>
  [x & forms]
  `(try
     (->> ~x ~@forms)
     (catch Throwable _#)))

(defn multi-comp
  ([fns a b]
    (multi-comp fns < a b))
  ([[f & others :as fns] order a b]
    (if (seq fns)
      (let [result (compare (f a) (f b))
            f-result (if (= order >) (* -1 result) result)]
        (if (= 0 f-result)
          (recur others order a b)
          f-result))
      0)))

(defmacro with-out-str-data-map
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*out* s#]
       (let [r# ~@body]
         {:result r#
          :str    (str s#)}))))
