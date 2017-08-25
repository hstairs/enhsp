;this instance is solvable by opening twice valves a and c. The first one put liquid from c to b (putting its value to 10) but at the same time
;it put c = 10. The second application puts the value of b to 10, then satisfying the goal condition.
(define 
    (problem instance1)
    (:domain pouring)
    (:objects a b -tank)
    (:init
        (= (volume a) 100)
        (= (volume b) 0)
        (= (flow-constant a) 0.8)
        (= (capacity a) 150)
        (= (capacity b) 150)
	    (connected a b)
    )
    (:goal 
	    (and
            (>= (volume b) 100)
	    )
    )
)
