(ns clojure-code.core
  (:gen-class))





;HW QUESTION 1:


(def inputs '(15 21 24 30 49))

(defn prime-num-else?
  "Determine if a number is prime by 
   iterating recursively through divisors"
  [x]
  (loop [iter 5
         top (Math/sqrt x)]
    (cond
      (> iter top) true
      (or (zero? (rem x iter))
          (zero? (rem x (+ 2 iter)))) false
      :else (recur (+ 6 iter) top))))



(defn is-prime?
  "Determines if a given integer is prime."
  [x]
  (cond
    (<= x 3) (< 1 x)
    (or (zero? (rem x 2))
        (zero? (rem x 3))) false
    :else (prime-num-else? x)))



(def prime-numbers (filter is-prime? (range 1 (+ (apply max inputs) 1))))

prime-numbers
;---------------------------------------------;

(defn prime-factor [n prime-num]
  (let [prime-filter (filter #(>= n %) prime-num)]
    (cond
      (or (zero? n) (empty? prime-filter)) '()
      (= (rem n (first prime-filter)) 0)
      (cons (first prime-filter)
            (prime-factor
             (/ n (first prime-filter)) prime-filter))
      :else (recur n (rest prime-filter)))))

(prime-factor 15 prime-numbers)

(defn key-val-creator [input prime-numbers]
  (map #(vector % input) prime-numbers))

(key-val-creator 15 (prime-factor 15 prime-numbers))


(defn mapper [f lat]
  (cond
    (empty? lat) '()
    :else (partition 2
                     (flatten
                      (cons (distinct (key-val-creator
                                       (first lat)
                                       (f (first lat) prime-numbers)))
                            (mapper f (rest lat)))))))


(defn sorter [key-val]
  (->> key-val
       (sort-by first)
       (partition-by first)
       (map #(vector (first (first %)) (map second %)))))



(defn reducer-helper [lat]
  (let [[vec] lat]
    [(first vec) (reduce + (second vec))]))


(defn reducer [lat]
  (cond
    (empty? lat) '()
    :else (cons (reducer-helper lat) (reducer (rest lat)))))


(reducer (sorter (mapper prime-factor inputs)))

(defn mapreduce [inputs mapper reducer]
  (reducer
   (sorter
    (mapper prime-factor inputs))))


(mapreduce inputs mapper reducer) ; final output functional style


(defn mapreduce-thread [inputs] ; final output thread
  (->> inputs
       (mapper prime-factor)
       sorter
       reducer))

(mapreduce-thread inputs)



; QUESTION 2:

(def doc1 (clojure.string/split "ABRACADABRA" #""))
(def doc2 (clojure.string/split "BRICABRAC" #""))



(defn shingle-set [doc]
  "At this moment, 
   functions as Bigram"
  (cond
    (empty? (rest doc)) '()
    :else (distinct (cons (str (first doc) (second doc))
                          (shingle-set (rest doc))))))

; ANTWORT to SUBQ A und SUBQ B
; Assumption: Set, not bag.

(shingle-set doc1)
(shingle-set doc2)
(count (shingle-set doc1)) ;7 
(count (shingle-set doc2)) ; 7 


;create insertection function
(defn member? [a lat]
  (cond
    (empty? lat) false
    (= (first lat) a) true
    (list? (first lat))
    (cond
      (= (first lat) a) true
      :else (recur a (first lat)))
    :else (recur a (rest lat))))


(defn intersect [s1 s2]
  "Count all 1s or 1s of same pos
   between s1 and s2?"
  (cond
    (empty? s1) '()
    (member? (first s1) s2) (cons 1 (intersect (rest s1) s2))
    :else (cons 0 (intersect (rest s1) s2))))

;do i need to make a intersect by position?

(intersect (shingle-set doc1) (shingle-set doc2))
(intersect (shingle-set doc2) (shingle-set doc1))

(shingle-set doc2)
; for SubQ C:
(reduce + (intersect (shingle-set doc1) (shingle-set doc2))) ; 5, if reduce is correct
(reduce + (intersect (shingle-set doc2) (shingle-set doc1))) ; 5, if reduce is correct


(defn union [s1 s2]
  (cond
    (empty? s1) s2
    :else (cons (first s1) (union (rest s1) s2))))


(defn jaccard-sim [s1 s2]
  "x/x+y
   or is it 
   (/ (distinct intersect) (count (distinct (union doc1 doc2))))?"
  (let [doc1 (shingle-set s1)
        doc2 (shingle-set s2)
        intersection (intersect doc1 doc2)]
    (/ (reduce + intersection) (count (distinct (union doc1 doc2))))))

(jaccard-sim doc1 doc2) ; 5/9

(defn jaccard-distance [jaccard-sim]
  "Gives the mettric of dissimilarity"
  (- 1 jaccard-sim))

(jaccard-distance (jaccard-sim doc1 doc2))



; QUESTION 4

(defn hash1 [x]
  (mod (+ (* 2 x) 1) 6))


(defn hash2 [x]
  (mod (+ (* 3 x) 2) 6))


(defn hash3 [x]
  (mod (+ (* 5 x) 2) 6))

(map hash3 (range 0,6))

(def char-matrix '((0 0 1 0 0 1)
                   (1 1 0 0 0 0)
                   (0 0 0 1 1 0)
                   (1 0 1 0 1 0)))

(defn find-1st-one [lat]
  "Finds first 1's position"
  (cond
    (empty? lat) 0
    (not (= (first lat) 1)) (+ 1 (find-1st-one (rest lat)))
    :else 0))


(defn hash-value [hash matrix]
  (cond
    (empty? matrix) '()
    :else (cons
           (hash (find-1st-one (first matrix)))
           (hash-value hash (rest matrix)))))


(defn hash-matrix [hash-list signature-f]
  (cond
    (empty? hash-list) '()
    :else
    (cons (signature-f (eval (first hash-list)) char-matrix)
          (hash-matrix (rest hash-list) signature-f))))


(hash-matrix '(hash1 hash2 hash3) hash-value)


(defn transpose [lat]
  "Transposes a matrix column into rows"
  (apply map list lat))


(transpose char-matrix)

; TASK: MAKE MIN HASH MATRIX, YA FILTHY ANIMAL


; Question 4 B


(def sig-m '((5 1 1 1)
             (2 2 2 2)
             (0 1 4 0)))

(def signature-matrix (transpose sig-m))

(def S1 (first signature-matrix))
(def S2 (second signature-matrix))
(def S3 (first (rest (rest signature-matrix))))
(def S4 (first (rest (rest (rest signature-matrix)))))

