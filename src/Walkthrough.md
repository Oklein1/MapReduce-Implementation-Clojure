## CS628- Data Mining
## Assignment 1: MapReduce & Shingling
#### By Ofir Klein
___Disclaimer:___ All code is written in Clojure. 

---
<br>
<br>


### Question 1:
1.	Suppose our input data to a MapReduce operation consists of integer values. The map function takes an integer i and produces the list of pairs (p,i) such that p is a prime divisor of i. For example, Map(12)=[(2,12),(3,12)].
The reduce function is defined to be “addition”. That is, Reduce (p, [i1,i2,….,ik]) is (p, i1+i2+…+ik). Compute the output if the input is the set of integers 15, 21,24, 30, 49. Elaborate on your response, make sure you explicitly show the steps including Map step, group by keys, and reduce step. (__25pts__)


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

**And with that, we have come to the end of question 1.**

---

### Question 2:

Find the set of 2-shingles for the "document": ABRACADABRA and also for the "document": BRICABRAC
Now, answer the following questions:

A. How many 2-shingles does ABRACADABRA have?
B. How many 2-shingles does BRICABRAC have?
C. How many 2-shingles do they have in common?
D. What is the Jaccard similarity between the two documents"? (25pts)

---
<br>
<br>

**Code used for answering Questions A-D:**

```clojure
(def doc1 (clojure.string/split "ABRACADABRA" #""))
; Output: ["A" "B" "R" "A" "C" "A" "D" "A" "B" "R" "A"]

(def doc2 (clojure.string/split "BRICABRAC" #""))
;["B" "R" "I" "C" "A" "B" "R" "A" "C"]

(defn shingle-set [doc]
  "At this moment, 
   functions as Bigram"
  (cond
    (empty? (rest doc)) '()
    :else (distinct (cons (str (first doc) (second doc))
                          (shingle-set (rest doc))))))
```

*Short summary of above code:* the <code>shingle-set</code> function creates a set of two-shingles. It concatenates the <code>first</code> and <code>second</code> element of the vector (array in other programming languages), then moves onto the next element of the vector. The function will terminate at the final element of the vector, since one cannot create a two-single element from it. To preserve the vector is popularted by unqie elements, I used Clojure's <code>distinct</code> function to remove any duplicates.
<br>

**Answer A:**

```clojure
(shingle-set doc1) ; Output: ("AB" "BR" "RA" "AC" "CA" "AD" "DA")
(count (shingle-set doc1)) ; ANSWER: 7
```
- <code>doc1</code> has **7** two-shingles.
<br>


**Answer B:**

```clojure
(shingle-set doc2) ; Output: ("BR" "RI" "IC" "CA" "AB" "RA" "AC")
(count (shingle-set doc2)) ; ANSWER: 7
```
- <code>doc2</code> has **7** two-shingles.
<br>


**Answer C:**
- There are **5** two-shingles in common between <code>doc1</code> and <code>doc2</code>.


```clojure
(defn member? [a lat]
"Checks if a is a member of lat"
  (cond
    (empty? lat) false
    (= (first lat) a) true
    (list? (first lat))
    (cond
      (= (first lat) a) true
      :else (recur a (first lat)))
    :else (recur a (rest lat))))

(defn intersect [s1 s2]
"Returns 1 where element of s1 is a member of s2,
      and returns 0 if there is no match"
  (cond
    (empty? s1) '()
    (member? (first s1) s2) (cons 1 (intersect (rest s1) s2))
    :else (cons 0 (intersect (rest s1) s2))))

(intersect (shingle-set doc1) (shingle-set doc2)); Output: (1 1 1 1 1 0 0)

(reduce + (intersect (shingle-set doc1) (shingle-set doc2))); ANSWER: 5.
```
<br>

Because The above code will also be used for the next question, allow me to provide a brief summary of these functions:

At the core of the question of intersection lies a simpler, far more fundamental one: that of membership. When comparing two sets, we ask "for each element within set1, is that element a *member* of s2?" Questions concerning insertion, removal, and even updating all derive their foundational structure from that of membership. And, for our purposes, the question concerning membership allows for higher-order concrescent functions, like intersection.

<br>

Now with respect to the <code>member?</code>, it is a predicate function (returning *truthy* or *falsey* value), taking in two arguments. The first <code>a</code> represents a string and <code>lat</code> represents a collection. At the core of this function, it uses recursion to check each element of the list. 
- *As an aside:* The membership function I built can also check any level of nesting that may exist in the collection.

With <code>member?</code> ready-to-hand, we can no2 built the <code>intersect</code> function. The basic tenant of this function is quite simple. We will use recursion to check each element of set1. In doing so, we shall ask if that element is a member of set2. If it is, return 1. If it is not a member, then return 0. What this function yields is a list of binary elements.

<br>

**Answer D:**
- The Jaccard Similarity is **5/9** (**~55.56%**)

With the creation of the <code>intersection</code> function, we are nearly halfway done with building the Jaccard similarity function.


As discussed in class, the Jaccard Similarity is written as follows: |S1⋂S2|/|S1⋃S2|. What is left is to create a union function:

```clojure
(defn union [s1 s2]
"Builds a list containing 
all elements from S1 and S2"
  (cond
    (empty? s1) s2
    :else (cons (first s1) (union (rest s1) s2))))
```

<br>

Finally, we can now proceed to build the <code>jaccard-sim</code> (Jaccard Similarity) function.

```clojure
(defn jaccard-sim [s1 s2]
  "x/x+y
   or is it 
   (/ (distinct intersect) (count (distinct (union doc1 doc2))))?"
  (let [doc1 (shingle-set s1)
        doc2 (shingle-set s2)]
    (/ (reduce + (intersect doc1 doc2)) (count (distinct (union doc1 doc2)))))) 
```

The above function begins by performing some local scoping. For example, the name <code>doc1</code> refers the output of the function, <code>shingle-set</code>, operating on the input argument <code>s1</code>. In terms of readability, after the <code>let</code> operator followed by the first bracket, all elements on the left hand side <code>doc1</code> and <code>doc2</code> are locally scoped names corresponding the the functions' output, lying adjacent to them.

In the final line of the <code>jaccard-sim</code> function, we find:

```clojure
(/ (reduce + (intersect doc1 doc2)) (count (distinct (union doc1 doc2))))
```
For the <intersection> function, we merely add all the 1s together to get 5, since there are 5 elements that intersect. With union, we join the list of shingles from <code>doc1</code> and <code>doc2</code>. We then use the <code>distinct</code> function to turn our list into a set. Therefrom, we count each element therein. As a result, we have 9. Finally, we perform division with the <code>/</code> operator and derive the results:

```clojure
(jaccard-sim doc1 doc2) ; Output: 5/9
```

<br>

----

### Question 3:

Consider the following matrix:


| Row|	C1  |	C2  |	C3 |	C4  |
|----|:----:|:---:|----:|----:|
| R1 |	0   |	 1  |	 1  |	 0  |
| R2 |	1   |	 0  |	 1  |	 1  |
| R3 |	0   |	 1  |	 0  |	 1  |
| R4 |	0   |	 0  |	 1  |	 0  |
| R5 |	1   |	 0  |	 1  |	 0  |
| R6 |	0   |	 1  |  0  |	 0  |


Perform a minhashing of the data, with the order of rows: R4(1), R6(2), R1(3), R3(4), R5(5), R2(6) and provide the minhash value for each column and signature created by this min-hashing function. Explain and elaborate on your response. (**25pts**)

<br>

<h4> Answer:</h4>

|Min-Hash|	C1  |	 C2 |	 C3 |	 C4 |
|--------|:----:|:---:|----:|----:|
|  h(x)  |	R5  |	 R6 |	 R4 |	 R3 |

<br>

**Explination:**

To derive this table, one must first follow the order of rows. As instruced, we begin with *R4*, which is first in the sequence. We then check to see its value at *C1*. If we find a 1, then we would place, in this instance, *R4* in our min-hash table under the column *C1*. But, if we find a zero, we now move to the second item in our order: *R6*

For shorthand, I will now reference this process as M(R,C). M represents for the matrix; R represents the row, and C refers to the column. Continuing out algorithm, we find that M(R6,C1) = 0. Therefore we follow the next sequence in the order. To skip a bit ahead, we eventually come the M(R5,C1) = 1. Finally! We have a 1 in our column. We now place *R5* in our new table under the column *C1*.

We now return to the beginning of our order of rows sequence, i.e. R4, R6, R1, R3, R5, and R6, but this time move to *C2*. We work our way through the order of rows until we find the first 1 value. Thereafter, we plug it into the table above, then repeat the process for *C3*, and so on.


-----


### Question 4:


For the matrix given below
(**a**)	Compute the minhash signature for each column if we use the following three hash functions: h1(x)=2x+1 mod 6; h2(x)=3x+2 mod 6; and h3(x)=5x+2 mod 6, where x represents the row index.

(**b**)	How close are the estimated Jaccard similarities for the six pairs of columns to the true Jaccard similarities? (**25pts**)


|Row |	S1 |	S2|	S3| S4|
|---|----|----|---|-----|
|0	| 0	 | 1	| 0	|  1  |
|1	| 0	 | 1	| 0	|  0  |
|2	| 1	 | 0	| 0	|  1  |
|3	| 0	 | 0	| 1	|  0  |
|4	| 0	 | 0	| 1	|  1  |
|5	| 1	 | 0	| 0	|  0  |
<br>
<br>


**Answer for A**:
<br>

|Row |	S1 |	S2|	S3| S4| h1 | h2 | h3 |
|---|----|----|---|-----| ---| ---| ---|
|0	| 0	 | 1	| 0	|  1  |  1 |  2 |  2 |
|1	| 0	 | 1	| 0	|  0  |  3 |  5 |  1 |
|2	| 1	 | 0	| 0	|  1  |  5 |  2 |  0 |
|3	| 0	 | 0	| 1	|  0  |  1 |  5 |  5 |
|4	| 0	 | 0	| 1	|  1  |  3 |  2 |  4 |
|5	| 1	 | 0	| 0	|  0  |  5 |  5 |  3 |

<br>
<br>
<rb>

**High-level summarization of algorithm:**

First, I computed hash1, hash2, and hash3, as instructed. The results can be found in the above graph.

To compute the hash function, I provided a visualization of every step in the form of tables: Steps 1-7.


- **Step 2**: We look to row 0 and check to see if there exists any 1 values at S1, S2, S3, and S4. S2 and S4 both have a 1 value, so we check each hash function and ask, "is the hash function less than the current value in the signature hash matrix?" Because 1 and 2 are less than infinity, we replace the infinite symbol with the hash value at S2 and S4. Now move to the next row.
- **Step 3:** Looking at row1, we find S2 is the only column with a 1. We look to see the current values in the signature min-hash table. We now compare the h1, h2, and h3 value against what is currently in each cell at column S2. The only change we make is to h3 because h3's value is a 1 and 1 is less than 2, so we replace the current value with 1. We do not do this to S4, because a 0 populates the cell at row1.
- **Step 4-6:** This process continues until we have exhausted every row. Again the main tenants of the algorithm are:
  1. computer all hash values
  2. For each column check the value.
     1. if column c has 0 in row r, then we do nothing.
     2. Otherwise, we look to see the current value in our signature Min-hash matrix. We compare the current value against the hash function and take the smaller of the two. 

<br>
<br>

**Step 1:**

|Min-Hash|	S1  |	 S2 |	 S3 |	 S4 |
|--------|:----:|:---:|----:|----:|
|  h1(x)  |	∞  |	 ∞ |	 ∞ |	 ∞  |
|  h2(x)  |	∞  |	 ∞ |	 ∞ |	 ∞  |
|  h3(x)  |	∞  |	 ∞ |	 ∞ |	 ∞  |

<br>

**Step 2, Row 0:** 

|Min-Hash|	S1  |	 S2 |	 S3 |	 S4 |
|--------|:----:|:---:|----:|----:|
|  h1(x)  |	∞  |	 1 |	 ∞ |	 1  |
|  h2(x)  |	∞  |	 2 |	 ∞ |	 2  |
|  h3(x)  |	∞  |	 2 |	 ∞ |	 2  |

<br>

**Step 3, Row 1:**
|Min-Hash|	S1  |	 S2 |	 S3 |	 S4 |
|--------|:----:|:---:|----:|----:|
|  h1(x)  |	∞  |	 1 |	 ∞ |	 1  |
|  h2(x)  |	∞  |	 2 |	 ∞ |	 2  |
|  h3(x)  |	∞  |	 1 |	 ∞ |	 2  | 



<br>

**Step 4, Row 2:**

|Min-Hash|	S1  |	 S2 |	 S3 |	 S4 |
|--------|:----:|:---:|----:|----:|
|  h1(x)  |	5  |	 1 |	 ∞ |	 1  |
|  h2(x)  |	2  |	 2 |	 ∞ |	 2  |
|  h3(x)  |	0  |	 1 |	 ∞ |	 0  | 

<br>

**Step 5, Row 3:**

|Min-Hash|	S1  |	 S2 |	 S3 |	 S4 |
|--------|:----:|:---:|----:|----:|
|  h1(x)  |	5  |	 1 |	 1 |	 1  |
|  h2(x)  |	2  |	 2 |	 5 |	 2  |
|  h3(x)  |	0  |	 1 |	 5 |	 0  |

<br>

**Step 6, Row 4:**

|Min-Hash|	S1  |	 S2 |	 S3 |	 S4 |
|--------|:----:|:---:|----:|----:|
|  h1(x)  |	5  |	 1 |	 1 |	 1  |
|  h2(x)  |	2  |	 2 |	 2 |	 2  |
|  h3(x)  |	0  |	 1 |	 4 |	 0  |

<br>

**Step 7, Row 5: Final Output**



|Min-Hash|	S1  |	 S2 |	 S3 |	 S4 |
|--------|:----:|:---:|----:|----:|
|  h1(x)  |	5  |	 1 |	 1 |	 1  |
|  h2(x)  |	2  |	 2 |	 2 |	 2  |
|  h3(x)  |	0  |	 1 |	 4 |	 0  |







<br>
<br>
<br>

<h4>Answer for B:</h4>

<br>

####Original Matrix Comparisons: 


|        |C1&C2           |	 C1&C3        | C1&C4        | C2&C3           | C2&C4      | C3&C4    |
|--------|:-------:|:---:|-----:|------:| -----| ---------| 
|  S1⋂S2|       	{ }     |	  { }         |	 {2}         |	 { }           |   {0}      |   {4}    | 
|  S1⋃S2| 	{0, 1, 2, 5}  |	 {2, 3, 4, 5} |	 {0, 2, 4, 5}|	 {0, 1, 3, 4}  |{0, 1, 2, 4}| {0,2,3,4}| 
| Abs S1⋂S2|	    0       |	 0            |	 1           |	 0             |    1       |   1      | 
| Abs S1⋃S2|	    3       |	 4            |	 4           |	 4             |   4        |   4      | 
| Jaccard Similarity|	0   |	 0            |	 1/4 (25%)   |	 0             |  1/4 (25%) |1/4 (25%) |  

<br>

####Signature-Matrix Comparisons: 



|        |C1&C2           |	 C1&C3        | C1&C4        | C2&C3           | C2&C4      | C3&C4    |
|--------|:----:|:-----:|----------:|---------:| ------|--------| 
|  S1⋂S2|     {hash2}        |        {hash2}   |  {hash2, hash4}|{hash1, hash2}|{hash1, hash2}| {hash1, hash2}| 
|  S1⋃S2| {hash1,hash2,hash3}|	 {hash1,hash2,hash3} |	 {hash1,hash2,hash3}|	 {hash1,hash2,hash3}  |{hash1,hash2,hash3}| {hash1,hash2,hash3}| 
| Abs S1⋂S2|	    1       |	 1            |	 2           |	 2             |    2       |   2      | 
| Abs S1⋃S2|	    3       |	 3            |	 3           |	 3             |   3        |   3      |
| Jaccard Similarity|	1/3   |	 1/3   |	 2/3   |	 2/3 |  2/3 | 2/3 |  

<br>
<br>
<br>

<h3>Final calculations:</h3>


<br>

|        |C1&C2           |	 C1&C3        | C1&C4        | C2&C3           | C2&C4      | C3&C4    |
|--------|:------:|:------:|------:|-------:| -----------| ---------| 
|  Original Matrix  |	0  |	 0 |	 0.25 |	 0  | 0.25| 0.25|
|  Signature Matrix  |	1/3  |	 1/3 |	 2/3 |	 2/3  |   2/3 | 2/3 |
|  Estimated Similarity  |	0.3333  |	 0.3333 |	 0.416667 |	 0.666667  | 0.416667 | 0.416667 |