### Question 1:
1.	Suppose our input data to a MapReduce operation consists of integer values. The map function takes an integer i and produces the list of pairs (p,i) such that p is a prime divisor of i. For example, Map(12)=[(2,12),(3,12)].
The reduce function is defined to be “addition”. That is, Reduce (p, [i1,i2,….,ik]) is (p, i1+i2+…+ik). Compute the output if the input is the set of integers 15, 21,24, 30, 49. Elaborate on your response, make sure you explicitly show the steps including Map step, group by keys, and reduce step.


<br>
<br>

__Answer:__


Before providing an explanation of my code, I will start from my <code>mapreduce-thread</code> function, describe in brief each step, and then proceed backwards, explaining each function therein.

```clojure
(defn mapreduce-thread [inputs] 
  (->> inputs
       (mapper prime-factor)
       sorter
       reducer))

(mapreduce-thread inputs) ;calling thread function
```

      Output: ([2 54] [3 90] [5 45] [7 70]) ; Answer for Q1.


<br>
For purposes of readability, I have provided my <code>mapreduce-thread</code> function in a thread format. It should be read starting from <code>inputs</code> and ending with <code>reducer</code>. The function is read as follow:

1. The function first received the desired input list, as specified in the instructions. 
2. The input is threaded to the <code>mapper</code> function that takes in two arguments: <code>prime-factor</code> function and the <code>inputs</code>. As an output, it produces a list of pairs: a the prime divisor and the input integer. 
3.  The list of pairs are fed into the <code>sorter function</code>. As an output, it constructs a new pair, as shown below. It takes the first sorts each pair based on the prime divisor, partitions in the pairs, then creates a series of pairs: the first element in the pair represents the prime divisor, while the second element is a list containing all input integers that contain that prime divisor:
    1.  Example: <code>([2 (24 30)] [3 (15 21 24 30)] [5 (15 30)] [7 (21 49)])</code>
4. Finally, we arrive at the <code>reducer</code> function. The <code>reducer</code> function adds all elements within list of input integers
<br>

**For what is to follow,** I will explain each part of my code, starting with the preparatory step of generating prime numbers. Thereafter I will discuss the <code>map</code>, <code>sort</code>, and <code>reduce</code> functions respectively.

<br>

#### Preparatory Step: Generating a Set of Prime Numbers.

**The first problem** in need of solving is to derive all prime divisors of the input integer.


In order to construct a prime-divisor-input-integer pair, one has to first create a set of prime numbers. To determine the necessary amount of prime numbers needed in the collection, I derived all prime divisors less than the maximum value located in the <code>inputs</code> object: e.g. 49. 

Pragmatically, the steps to achieving this are remarkably simple: generate a collection of items from 1 to, in this case, 49 (the maximum value in our set). Use Clojure's <code>filter</code> function to generate a new set of all primary numbers. How does it do this? <code>filter</code> takes two arguments: a predicate function (a function returning a *truthy* or *falsey* value), and a collection of items, e.g. a list. This is how <code>filter</code>, well, filters through a collection: it looks through each element of the collection and, in this case, asks "is this number prime?" If the value returned is true, then the numeric is adjoined to a new collection (of all prime numbers). If not, then onto the next element. 


<br> 

``` clojure
(def inputs '(15 21 24 30 49))

(defn prime-num-else? ;handles numerics in the tens of thousands
  "Determines if a number is prime by 
   looping (recursively) through the divisors"
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
```


If we call the value for the object <code>prime-numbers</code> we will see: 

     OUTPUT: (1, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47)


----

#### Mapper function:

**Goal of Mapper**: The goal of this function is to construct and produce a list of pairs (p,i) such that p is a prime divisor of i, as noted in the instructions.
<br>

**Approach of explanation:** Allow me to introduce the entire code snippet for the <code>mapper</code> function. I will then provide high-level exposition of each part of it, beginning with the <code>mapper</code>, moving then to the <code>prime-factor</code> and <code>key-val-creator</code> functions.
<br>

When calling the <code>mapper</code> function, it takes two inputs: a function (<code>f</code>) and a list (<code>lat</code>), representing the object called <code>inputs</code>. 

The thinking behind this function is as follows: the <code>mapper</code> function will recursively call each element in <code>input</code>. In each instance, a closure, the function,<code>key-val-creator</code>, is utilized to construct the the key-val set. Because, as it was mentioned in class, we want unique set pairs, I utilized clojure's <code>distinct</code> function before constructing a pair with Lisp's infamous <code>cons</code> function. 
- **Note**: I used two additional functions: <code>flatten</code> and <code>partition</code>. My code created a series of nested lists; all in the correct order and with the correct values. The task was then to extract them, and structure them as a key-value pair. <code>flatten</code> extracts the values values from nested lists and returns all values to a single list. Given that all the values were in the correct order and sequence, I just had to use <code>partition</code> and partition the new list by increments of two. This formatted the key-value pairs in the intended data structure and order.

Now, it's important to take note of the <code>prime-factor</code> and <code>key-val-creator</code>; this is where the magic really happens.

- When we call the <code>prime-factor</code> function, it will create a list (collection of items) of all prime numbers for the given input. For example, in the first instance, 15 is the initial input represented by the argument "*n*". The function returns all prime divisors. 

  ```clojure
  (prime-factor 15 prime-numbers) ; OUTPUT: (3,5)
  ```

- When we call the <code>key-value-creator</code> function below, it will take two arguments: the initial integer, —in the previous example, 15,— and the list of primary divisors created from the <code>prime-factor</code> function. It will then create a key-value pair, as shown below:
  ```clojure
  (key-val-creator 15 (prime-factor 15 prime-numbers)) ; OUTPUT ([3 15] [5 15])
  ```

<br>
<br>

In summary, the mapper function the <code>mapper</code> function utilizes the <code>prime-factor</code> to derive the primary divisors of each input value. After that, a closure is used, i.e. <code>key-value-creator</code>, to create key-value pairs where the key is the primary divisor and the value is the input at the instance of the call. This then prepares us to move into the next section: **sorting.**


<br>

```clojure
;Here is the full code of the aforementioned explanations

(defn prime-factor [n prime-num]
"Determines all prime factors of a given input"
  (let [prime-filter (filter #(>= n %) prime-num)]
    (cond
      (or (zero? n) (empty? prime-filter)) '()
      (= (rem n (first prime-filter)) 0)
      (cons (first prime-filter)
            (prime-factor
             (/ n (first prime-filter)) prime-filter))
      :else (recur n (rest prime-filter)))))


(defn key-val-creator [input prime-numbers]
  (map #(vector % input) prime-numbers))


(defn mapper [f lat]
  (cond
    (empty? lat) '()
    :else (partition 2
                     (flatten
                      (cons (distinct (key-val-creator
                                      (first lat)
                                      (f (first lat) prime-numbers)))
                       (mapper f (rest lat)))))))

(mapper prime-factor inputs); calling mapper function
```

        Output of mapper function: ((3 15) (5 15) (3 21) (7 21) (2 24) (3 24) (2 30) (3 30) (5 30) (7 49))

---

#### Sort function:

**Goal of Sort**: The <code>sorter</code> function (thread) goal is to compare all pairs by key, i.e. the primary divisor, aggregate them by key, and then create a new pair, where the key is the primary divisor and the value is a list of the values that are divisible by said divisor. 



```clojure
(defn sorter [key-val]
  (->> key-val
       (sort-by first)
       (partition-by first)
       (map #(vector (first (first %)) (map second %)))))
```

Allow me to provide examples by walking through the function. 

Because the data structure determines our circumstance in Clojure, it's of superlative importance to take great heed when planning out one's functions. Let's begin by providing the <code>mapper</code>'s output as the input for the <code>sorter</code> function

```clojure
(sort-by first (mapper prime-factor inputs))
```
Our output will be:
      
    ((2 24) (2 30) (3 15) (3 21) (3 24) (3 30) (5 15) (5 30) (7 21) (7 49))

As we can see, the entire list is sorted by the primary divisor, and group them by prime divisors using the <code>parition-by</code> function:

```clojure
(partition-by first (sort-by first (mapper prime-factor inputs)))
```

The output will now be:

    (((2 24) (2 30)) ((3 15) (3 21) (3 24) (3 30)) ((5 15) (5 30)) ((7 21) (7 49)))

If we count the number of partitioned lists-buckets, we will count 4.

Finally, when we use Clojure's <code>map</code> and anonymous function to construct our final pairs. The final output will look like this:

```clojure 
(sorter (mapper prime-factor inputs)) ; calling sorter function
```

    Output:([2 (24 30)] [3 (15 21 24 30)] [5 (15 30)] [7 (21 49)])

Here we can observe new pairings. The key refers to the prime divisor, and the value is a list of integers that contain the initial inputs divisible by said prime divisors.  

From here, we can move to the <code>reducer</code> function.

---

#### Reducer function:

**Goal of Reducer**: The <code>reducer</code> function's goal is simply to reduce all key elements within the list into a single numeric. To do this, <code>reducer</code> takes two arguments: an operator, in this case <code>+</code>, and a list.
-  __Note__: <code>reducer</code>'s input is derived from the output of the <code>sorter</code> function.

To construct the full <reducer> function, I separated out the operation and recursive iteration into two functions. <code>reducer-helper</code> does the heavy work;  <code>reducer-helper</code> uses Clojure's deconstruction syntax to extract the first list of the sequence only. At the first instance, <code>vec</code> refers to <code>[2 (24 30)]</code>. The first element is the primary divisor, while the second is the sum of 24 and 30.

But, what of all the three other groups (lists)? The <code>reducer</code> function handles this; it iterates through the remaining elements in the list by means of recursion. 
<br>

```clojure
(defn reducer-helper [lat]
  (let [[vec] lat]
    [(first vec) (reduce + (second vec))]))


(defn reducer [lat]
  (cond
    (empty? lat) '()
    :else (cons (reducer-helper lat) (reducer (rest lat)))))
```

Once complete, the output will look as follows:

```clojure
(reducer (sorter (mapper prime-factor inputs)))
```

    Output: ([2 54] [3 90] [5 45] [7 70])
<br>
<br>
